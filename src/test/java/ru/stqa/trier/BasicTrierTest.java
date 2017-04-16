/*
 * Copyright 2017 Alexei Barantsev
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package ru.stqa.trier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BasicTrierTest {

  private Trier trier;

  @BeforeEach
  void init() {
    trier = new CounterBasedTrier(5, new TestingClock(), 1L);
  }

  @Test
  void shouldNotAllowToResetIgnoredExceptions() {
    trier.ignoring(NumberFormatException.class);
    assertThrows(IllegalStateException.class,
      () -> trier.ignoring(NumberFormatException.class));
  }

  @Test
  void shouldNotAllowToResetIgnoredResult() {
    trier.ignoring(res -> res.equals("FAIL"));
    assertThrows(IllegalStateException.class,
      () -> trier.ignoring(res -> res.equals("FAIL")));
  }

  @Test
  void shouldNotAllowToResetIgnoredResultWithUnexpectedResult() {
    trier.ignoring(res -> res.equals("FAIL"));
    assertThrows(IllegalStateException.class,
      () -> trier.until(res -> res.equals("OK")));
  }

  @Test
  void shouldNotAllowToResetUnexpectedResult() {
    trier.until(res -> res.equals("OK"));
    assertThrows(IllegalStateException.class,
      () -> trier.until(res -> res.equals("OK")));
  }

  @Test
  void shouldNotAllowToResetUnexpectedResultWithIgnoredResult() {
    trier.until(res -> res.equals("OK"));
    assertThrows(IllegalStateException.class,
      () -> trier.ignoring(res -> res.equals("FAIL")));
  }

}
