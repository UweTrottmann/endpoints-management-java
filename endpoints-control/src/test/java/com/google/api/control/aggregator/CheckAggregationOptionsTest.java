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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.google.common.cache.Cache;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;

/**
 * Tests for CheckAggregationOptions.
 */
@RunWith(JUnit4.class)
public class CheckAggregationOptionsTest {

  @Test
  public void defaultConstructorShouldSpecifyTheDefaultValues() {
    CheckAggregationOptions options = new CheckAggregationOptions();
    assertEquals(CheckAggregationOptions.DEFAULT_NUM_ENTRIES, options.getNumEntries());
    assertEquals(CheckAggregationOptions.DEFAULT_RESPONSE_EXPIRATION_MILLIS,
        options.getExpirationMillis());
  }

  @Test
  public void shouldFailToCreateCacheWithANullOutputDeque() {
    try {
      CheckAggregationOptions options = new CheckAggregationOptions();
      options.createCache(null);
      fail("should have raised NullPointerException");
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void shouldFailToCreateACacheWithANullTicker() {
    try {
      CheckAggregationOptions options = new CheckAggregationOptions();
      options.createCache(null);
      fail("should have raised NullPointerException");
    } catch (NullPointerException e) {
      // expected
    }
  }

  @Test
  public void shouldNotCreateACacheUnlessMaxSizeIsPositive() {
    for (int i : new int[] {-1, 0, 1}) {
      CheckAggregationOptions options = new CheckAggregationOptions(i,
              CheckAggregationOptions.DEFAULT_RESPONSE_EXPIRATION_MILLIS);
      if (i > 0) {
        assertNotNull(options.createCache());
      } else {
        assertNull(options.createCache());
      }
    }
  }

  @Test
  public void shouldCreateACacheEvenIfExpirationIsNotPositive() {
    for (int i : new int[] {-1, 0, 1}) {
      CheckAggregationOptions options =
          new CheckAggregationOptions(CheckAggregationOptions.DEFAULT_NUM_ENTRIES, i);
      assertNotNull(options.createCache());
    }
  }

  @Test
  public void shouldCreateACacheThatDeletesEntryAfterExpiration() {
    CheckAggregationOptions options =
        new CheckAggregationOptions(CheckAggregationOptions.DEFAULT_NUM_ENTRIES, 1);

    FakeTicker ticker = new FakeTicker();
    Cache<String, Long> cache = options.createCache(ticker);
    cache.put("one", 1L);
    assertEquals(1, cache.size());
    ticker.tick(1 /* expires the entry */, TimeUnit.MILLISECONDS);
    cache.cleanUp();
    assertEquals(0, cache.size());
  }

  private static ConcurrentLinkedDeque<Long> testDeque() {
    return new ConcurrentLinkedDeque<Long>();
  }
}
