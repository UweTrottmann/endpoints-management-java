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

import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

/**
 * Holds values used to configure check aggregation.
 */
public class CheckAggregationOptions {
  /**
   * The default aggregation cache size.
   */
  public static final int DEFAULT_NUM_ENTRIES = 1000;

  /**
   * The default response expiration interval.
   */
  public static final int DEFAULT_RESPONSE_EXPIRATION_MILLIS = 4000;

  private final int numEntries;
  private final int expirationMillis;

  /**
   * Constructor
   *
   * @param numEntries
   *            is the maximum number of cache entries that can be kept in the
   *            aggregation cache. The cache is disabled if this value is
   *            negative.
   * @param expirationMillis
   *            is the maximum interval in milliseconds before a cached check
   *            response is invalidated.
   */
  public CheckAggregationOptions(int numEntries, int expirationMillis) {
    this.numEntries = numEntries;
    this.expirationMillis = expirationMillis;
  }

  /**
   * No-arg constructor
   *
   * Creates an instance initialized with the default values.
   */
  public CheckAggregationOptions() {
    this(DEFAULT_NUM_ENTRIES, DEFAULT_RESPONSE_EXPIRATION_MILLIS);
  }

  /**
   * @return the maximum number of cache entries that can be kept in the
   *         aggregation cache.
   */
  public int getNumEntries() {
    return numEntries;
  }

  /**
   * @return the maximum interval before a cached check response should be
   *         deleted.
   */
  public int getExpirationMillis() {
    return expirationMillis;
  }

  /**
   * Creates a {@link Cache} configured by this instance.
   *
   * @param <T>
   *            the type of the instance being cached
   *
   * @return a {@link Cache} corresponding to this instance's values or
   *         {@code null} unless {@link #numEntries} is positive.
   */
  @Nullable
  public <T> Cache<String, T> createCache() {
    return createCache(Ticker.systemTicker());
  }

  /**
   * Creates a {@link Cache} configured by this instance.
   *
   * @param <T>    the type of the value stored in the Cache
   * @param ticker the time source used to determine expiration
   * @return a {@link Cache} corresponding to this instance's values or
   * {@code null} unless {@code #numEntries} is positive.
   */
  @Nullable
  public <T> Cache<String, T> createCache(Ticker ticker) {
    Preconditions.checkNotNull(ticker, "The ticker cannot be null");
    if (numEntries <= 0) {
      return null;
    }
    CacheBuilder<Object, Object> b = CacheBuilder.newBuilder().maximumSize(numEntries).ticker(ticker);
    if (expirationMillis >= 0) {
      b.expireAfterWrite(expirationMillis, TimeUnit.MILLISECONDS);
    }
    return b.build();
  }
}
