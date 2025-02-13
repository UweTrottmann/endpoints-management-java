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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

import com.google.api.servicecontrol.v1.CheckError;
import com.google.api.servicecontrol.v1.CheckError.Code;
import com.google.api.servicecontrol.v1.CheckRequest;
import com.google.api.servicecontrol.v1.CheckResponse;
import com.google.api.servicecontrol.v1.MetricValue;
import com.google.api.servicecontrol.v1.MetricValueSet;
import com.google.api.servicecontrol.v1.Operation;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.protobuf.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.TimeUnit;

/**
 * CheckRequestAggregatorTest tests the behavior in CheckRequestAggregator
 */
@RunWith(JUnit4.class)
public class CheckRequestAggregatorTest {
  private static final double TEST_DOUBLE_VALUE = 1.1;
  private static final String TEST_OPERATION_NAME = "aTestOperation";
  private static final String TEST_CONSUMER_ID = "testConsumerId";
  private static final String CACHING_NAME = "service.caching";
  private static final String DEFAULT_NAME = "service.default";
  private static final String NO_CACHE_NAME = "service.no.cache";
  private static final int TEST_FLUSH_INTERVAL = 1;
  private static final int TEST_EXPIRATION = TEST_FLUSH_INTERVAL + 1;
  private static final Timestamp EARLY = Timestamp.newBuilder().setNanos(1).setSeconds(100).build();
  private CheckRequestAggregator NO_CACHE = new CheckRequestAggregator(NO_CACHE_NAME,
      new CheckAggregationOptions(-1 /* disables cache */, 1));
  private CheckRequestAggregator DEFAULT =
      new CheckRequestAggregator(DEFAULT_NAME, new CheckAggregationOptions());
  private FakeTicker ticker;

  @Before
  public void createTicker() {
    this.ticker = new FakeTicker();
  }

  @Test
  public void signShouldFailOnInvalidCheckRequest() {
    Operation.Builder withConsumerId = Operation.newBuilder().setConsumerId(TEST_CONSUMER_ID);
    Operation.Builder withOperationName =
        Operation.newBuilder().setOperationName(TEST_OPERATION_NAME);
    CheckRequest[] invalidRequests = {
        CheckRequest.getDefaultInstance(), // No operation
        CheckRequest.newBuilder().setOperation(withConsumerId).build(), // no operation name
        CheckRequest.newBuilder().setOperation(withOperationName).build(), // no consumer ID
    };
    for (CheckRequest r : invalidRequests) {
      try {
        CheckRequestAggregator.sign(r);
        fail("Should have raised IllegalArgumentException");
      } catch (IllegalArgumentException e) {
        // expected
      }
    }
  }

  @Test
  public void signShouldChangeAsImportFieldsChange() {
    CheckRequest testRequest = newTestRequest(DEFAULT_NAME);
    HashCode initial = CheckRequestAggregator.sign(testRequest);

    // add labels
    CheckRequest.Builder b = testRequest.toBuilder();
    Operation.Builder ob = b.getOperation().toBuilder();
    ob.putAllLabels(ImmutableMap.of("key", "value"));
    HashCode withLabels = CheckRequestAggregator.sign(b.setOperation(ob).build());
    assertNotEquals(withLabels, initial); // signature with labels is different
    ob.addMetricValueSets(newDoubleMetricValueSet("some_doubles", TEST_DOUBLE_VALUE, EARLY));

    // metrics
    HashCode withMetrics = CheckRequestAggregator.sign(b.setOperation(ob).build());
    assertNotEquals(withMetrics, initial);
    assertNotEquals(withMetrics, withLabels);
  }

  @Test
  public void whenNonCachingShouldHaveWellKnownFlushInterval() {
    assertEquals(CheckRequestAggregator.NON_CACHING, NO_CACHE.getExpirationMillis());
  }

  @Test
  public void whenNonCachingShouldNotCacheResponse() {
    CheckRequest req = newTestRequest("service.no_cache");
    assertEquals(null, NO_CACHE.check(req));
    CheckResponse fakeResponse =
        fakeResponse();
    NO_CACHE.addResponse(req, fakeResponse);
    assertEquals(null, NO_CACHE.check(req));
    NO_CACHE.clear();
    assertEquals(null, NO_CACHE.check(req));
  }

  @Test
  public void whenCachingShouldFailForRequestsWithTheWrongServiceName() {
    try {
      DEFAULT.check(newTestRequest(DEFAULT_NAME + ".extra"));
      fail("Should have raised IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void whenCachingShouldFailForRequestsWithNoOperation() {
    try {
      DEFAULT.check(CheckRequest.getDefaultInstance());
      fail("Should have raised IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // expected
    }
  }

  @Test
  public void whenCachingShouldReturnNullInitiallyAsRequestIsNotCached() {
    CheckRequestAggregator agg = newCachingInstance();
    assertEquals(null, agg.check(newTestRequest(CACHING_NAME)));
  }

  @Test
  public void whenCachingShouldHaveExpiration() {
    CheckRequestAggregator agg = newCachingInstance();
    assertEquals(TEST_EXPIRATION, agg.getExpirationMillis());
  }

  @Test
  public void whenCachingShouldCacheResponses() {
    CheckRequest req = newTestRequest(CACHING_NAME);
    CheckRequestAggregator agg = newCachingInstance();
    CheckResponse fakeResponse =
        fakeResponse();
    assertEquals(null, agg.check(req));
    agg.addResponse(req, fakeResponse);
    assertEquals(fakeResponse, agg.check(req));
  }

  @Test
  public void shouldNotCacheRequestsWithImportantOperations() {
    CheckRequest req = newTestRequest(CACHING_NAME, Operation.Importance.HIGH);
    CheckRequestAggregator agg = newCachingInstance();
    CheckResponse fakeResponse =
        fakeResponse();
    assertEquals(null, agg.check(req));
    agg.addResponse(req, fakeResponse);
    assertEquals(null, agg.check(req));
  }

  @Test
  public void shouldExtendExpirationOnReceiptOfAResponse() {
    CheckRequest req = newTestRequest(CACHING_NAME);
    CheckRequestAggregator agg = newCachingInstance();
    CheckResponse fakeResponse = fakeResponse();
    assertEquals(null, agg.check(req));
    agg.addResponse(req, fakeResponse);
    assertEquals(fakeResponse, agg.check(req));

    ticker.tick(1, TimeUnit.MILLISECONDS);
    // until expiry, the response should be returned
    assertEquals(fakeResponse, agg.check(req)); // not expired yet
    assertEquals(fakeResponse, agg.check(req)); // not expired yet

    // add a response as the request expires, fake response continues to be returned
    ticker.tick(1, TimeUnit.MILLISECONDS);
    agg.addResponse(req, fakeResponse);
    assertEquals(fakeResponse, agg.check(req)); // still in cache
    assertEquals(fakeResponse, agg.check(req)); // really still in the cache

    // confirm that it was cached
    ticker.tick(1, TimeUnit.MILLISECONDS);
    // until expiry, the response should be returned
    assertEquals(fakeResponse, agg.check(req)); // not expired yet
    assertEquals(fakeResponse, agg.check(req)); // not expired yet
  }

  @Test
  public void shouldExpireRequestThatHasNotBeenUpdated() {
    CheckRequest req = newTestRequest(CACHING_NAME);
    CheckRequestAggregator agg = newCachingInstance();
    CheckResponse fakeResponse = fakeResponse();
    assertEquals(null, agg.check(req));
    agg.addResponse(req, fakeResponse);
    assertEquals(fakeResponse, agg.check(req));
    ticker.tick(TEST_EXPIRATION, TimeUnit.MILLISECONDS);

    // now expired, confirm nothing in the cache
    assertEquals(null, agg.check(req));
    assertEquals(null, agg.check(req));
  }

  @Test
  public void shouldNotExpireRequestThatHasBeenUpdated() {
    CheckRequest req = newTestRequest(CACHING_NAME);
    CheckRequestAggregator agg = newCachingInstance();
    CheckResponse fakeResponse = fakeResponse();
    assertEquals(null, agg.check(req));
    agg.addResponse(req, fakeResponse);
    assertEquals(fakeResponse, agg.check(req));
    ticker.tick(1, TimeUnit.MILLISECONDS);

    // update request
    agg.addResponse(req, fakeResponse);

    // would be expired if not updated
    ticker.tick(1, TimeUnit.MILLISECONDS);
    assertEquals(fakeResponse, agg.check(req));

    // now expired, confirm nothing in the cache
    ticker.tick(TEST_EXPIRATION, TimeUnit.MILLISECONDS);
    assertEquals(null, agg.check(req));
  }

  @Test
  public void shouldClearRequests() {
    CheckRequest req = newTestRequest(CACHING_NAME);
    CheckRequestAggregator agg = newCachingInstance();
    CheckResponse fakeResponse = fakeResponse();
    assertEquals(null, agg.check(req));
    agg.addResponse(req, fakeResponse);
    assertEquals(fakeResponse, agg.check(req));
    agg.clear();
    assertEquals(null, agg.check(req));
  }

  private CheckRequestAggregator newCachingInstance() {
    return new CheckRequestAggregator(CACHING_NAME,
        new CheckAggregationOptions(1, TEST_EXPIRATION), ticker);
  }

  private static CheckResponse fakeResponse() {
    return CheckResponse.newBuilder().setOperationId(TEST_OPERATION_NAME).build();
  }

  private static CheckRequest newTestRequest(String serviceName, Operation.Importance i) {
    if (i == null) {
      i = Operation.Importance.LOW;
    }
    Operation.Builder b = Operation
        .newBuilder()
        .setConsumerId(TEST_CONSUMER_ID)
        .setOperationName(TEST_OPERATION_NAME)
        .setImportance(i);
    return CheckRequest.newBuilder().setServiceName(serviceName).setOperation(b).build();
  }

  private static CheckRequest newTestRequest(String serviceName) {
    return newTestRequest(serviceName, Operation.Importance.LOW);
  }

  private static MetricValueSet newDoubleMetricValueSet(String name, double value,
      Timestamp endTime) {
    return MetricValueSet
        .newBuilder()
        .setMetricName(name)
        .addMetricValues(MetricValue.newBuilder().setDoubleValue(value).setEndTime(endTime))
        .build();
  }
}
