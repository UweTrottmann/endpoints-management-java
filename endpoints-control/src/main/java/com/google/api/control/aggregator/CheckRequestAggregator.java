/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 * Copyright 2023 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.api.control.aggregator;

import com.google.api.servicecontrol.v1.CheckRequest;
import com.google.api.servicecontrol.v1.CheckResponse;
import com.google.api.servicecontrol.v1.MetricValue;
import com.google.api.servicecontrol.v1.MetricValueSet;
import com.google.api.servicecontrol.v1.Operation;
import com.google.api.servicecontrol.v1.Operation.Importance;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;

/**
 * Caches {@link CheckRequest}s.
 */
public class CheckRequestAggregator {
  /**
   * The flush interval returned by {@link #getExpirationMillis() } when an instance is
   * configured to be non-caching.
   */
  public static final int NON_CACHING = -1;

  private final String serviceName;
  private final CheckAggregationOptions options;
  private final Cache<String, CachedItem> cache;
  private final Ticker ticker;

  /**
   * Constructor.
   *
   * @param serviceName the service whose {@code CheckRequest}s are being aggregated
   * @param options configures this instance's caching behavior
   * @param ticker the time source used to determine expiration. When not specified, this defaults
   *        to {@link Ticker#systemTicker()}
   */
  public CheckRequestAggregator(String serviceName, CheckAggregationOptions options,
                                @Nullable Ticker ticker) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(serviceName),
        "service name cannot be empty");
    Preconditions.checkNotNull(options, "options must be non-null");
    this.ticker = ticker == null ? Ticker.systemTicker() : ticker;
    this.cache = options.createCache(this.ticker);
    this.serviceName = serviceName;
    this.options = options;
  }

  /**
   * Constructor.
   *
   * @param serviceName the service whose {@code CheckRequest}s are being aggregated
   * @param options configures this instances caching behavior
   */
  public CheckRequestAggregator(String serviceName, CheckAggregationOptions options) {
    this(serviceName, options, Ticker.systemTicker());
  }

  /**
   * @return See {@link CheckAggregationOptions#getExpirationMillis()}.
   */
  public int getExpirationMillis() {
    if (cache == null) {
      return NON_CACHING;
    } else {
      return options.getExpirationMillis();
    }
  }

  /**
   * @return the service whose {@code CheckRequest}s are being aggregated
   */
  public String getServiceName() {
    return serviceName;
  }

  /**
   * Clears this instances cache of aggregated operations.
   *
   * Is intended to be called by the driver before shutdown.
   */
  public void clear() {
    if (cache == null) {
      return;
    }
    synchronized (cache) {
      cache.invalidateAll();
    }
  }

  /**
   * Adds the response from sending {@code req} to this instances cache.
   *
   * @param req a {@link CheckRequest}
   * @param resp the response from sending {@link CheckResponse}
   */
  public void addResponse(CheckRequest req, CheckResponse resp) {
    if (cache == null) {
      return;
    }
    String signature = sign(req).toString();
    long now = ticker.read();
    int quotaScale = 0; // WIP
    synchronized (cache) {
      CachedItem item = cache.getIfPresent(signature);
      if (item == null) {
        cache.put(signature, new CachedItem(resp, now, quotaScale));
      } else {
        item.lastCheckTimestamp = now;
        item.response = resp;
        item.quotaScale = quotaScale;
        item.isFlushing = false;
        cache.put(signature, item);
      }
    }
  }

  /**
   * Determine if a cached response corresponds to {@code req}.
   *
   * Determine if there are cache hits for the request in this instance as follows:
   *
   * <strong>Not in the Cache</strong> If {@code req} is not in the cache, it returns {@code null},
   * to indicate that the caller should send the request.
   *
   * <strong>Cache Hit, the response has errors</strong> When a cached response has errors, it's
   * assumed that {@code req}, would fail as well, so the cached response is returned. However, the
   * first request after the check interval has elapsed should be sent to the server to refresh the
   * response - until its response is received, the subsequent reqs should still return the failed
   * response.
   *
   * <strong>Cache Hit, the response passed</strong> When the cached response has no errors, it's
   * assumed that the {@code req} would pass as well, so the response is return, with quota tracking
   * updated so that it matches that in req.
   *
   * @param req a request to be sent to the service control service
   * @return a {@code CheckResponse} if an applicable one is cached by this instance, otherwise
   *         {@code null}
   */
  public @Nullable CheckResponse check(CheckRequest req) {
    if (cache == null) {
      return null;
    }
    Preconditions.checkArgument(req.getServiceName().equals(serviceName),
        String.format("service name mismatch. Aggregator service '%s', request service '%s'",
          serviceName, req.getServiceName()));
    Preconditions.checkNotNull(req.getOperation(), "expected check operation was not present");
    if (req.getOperation().getImportance() != Importance.LOW) {
      return null; // send the request now if importance is not LOW
    }
    String signature = sign(req).toString();
    CachedItem item = cache.getIfPresent(signature);
    if (item == null) {
      return null; // signal caller to send the response
    } else {
      return item.response;
    }
  }

  /**
   * Obtains the {@code HashCode} for the contents of {@code value}.
   *
   * @param value a {@code CheckRequest} to be signed
   * @return the {@code HashCode} corresponding to {@code value}
   */
  public static HashCode sign(CheckRequest value) {
    Hasher h = Hashing.md5().newHasher();
    Operation o = value.getOperation();
    if (o == null || Strings.isNullOrEmpty(o.getConsumerId())
        || Strings.isNullOrEmpty(o.getOperationName())) {
      throw new IllegalArgumentException("CheckRequest should have a valid operation");
    }
    h.putString(o.getConsumerId(), StandardCharsets.UTF_8);
    h.putChar('\0');
    h.putString(o.getOperationName(), StandardCharsets.UTF_8);
    h.putChar('\0');
    Signing.putLabels(h, o.getLabels());
    for (MetricValueSet mvSet : o.getMetricValueSetsList()) {
      h.putString(mvSet.getMetricName(), StandardCharsets.UTF_8);
      h.putChar('\0');
      for (MetricValue metricValue : mvSet.getMetricValuesList()) {
        MetricValues.putMetricValue(h, metricValue);
      }
    }
    return h.hash();
  }

  /**
   * CachedItem holds items cached along with a {@link CheckRequest}
   *
   * {@code CachedItem} is thread safe
   */
  private static class CachedItem {
    boolean isFlushing;
    long lastCheckTimestamp;
    int quotaScale;
    CheckResponse response;

    /**
     * @param response the cached {@code CheckResponse}
     * @param lastCheckTimestamp the last time the {@code CheckRequest} for tracked by this item was
     *        checked
     * @param quotaScale WIP, used to track quota
     */
    CachedItem(CheckResponse response, long lastCheckTimestamp, int quotaScale) {
      this.response = response;
      this.lastCheckTimestamp = lastCheckTimestamp;
      this.quotaScale = quotaScale;
    }

  }
}
