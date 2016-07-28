/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.api.auth;

import com.google.common.base.Ticker;

import java.time.Duration;

/**
 * A {@link Ticker} used for testing.
 *
 * @author yangguan@google.com
 *
 */
final class TestingTicker extends Ticker {
  private long clock = 0;

  @Override
  public long read() {
    return this.clock;
  }

  void advance(Duration timeToAdvance) {
    this.clock += timeToAdvance.toNanos();
  }
}
