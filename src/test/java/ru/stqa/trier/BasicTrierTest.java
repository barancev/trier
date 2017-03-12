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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BasicTrierTest {

  private TestingClock clock;
  private Trier trier;

  @BeforeEach
  void init() {
    clock = new TestingClock();
    trier = new CounterBasedTrier(5, clock, 1L);
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
