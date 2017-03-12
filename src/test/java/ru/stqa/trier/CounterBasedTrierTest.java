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

import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("CounterBasedTrier")
class CounterBasedTrierTest {

  TestingClock clock;
  Trier trier;

  @BeforeEach
  void init() {
    clock = new TestingClock();
    trier = new CounterBasedTrier(5, clock, 1L);
  }

  @Nested
  class BuildingTrier {

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

  @Nested
  class TryToRunRunnable {

    @Test
    void shouldReturnImmediatelyIfTheRunnableDoesNotThrow() throws InterruptedException {
      Runnable run = mock(Runnable.class);
      trier.tryTo(run);
      verify(run, times(1)).run();
      assertThat(clock.now(), is(0L));
    }

    @Test
    void shouldIgnoreIgnoredExceptionThrownOnce() throws InterruptedException {
      Runnable run = mock(Runnable.class);
      doThrow(NumberFormatException.class).doNothing().when(run).run();
      trier.ignoring(NumberFormatException.class).tryTo(run);
      verify(run, times(2)).run();
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldIgnoreIgnoredExceptionThrownSeveralTimes() throws InterruptedException {
      Runnable run = mock(Runnable.class);
      doThrow(NumberFormatException.class).doThrow(NumberFormatException.class).doNothing().when(run).run();
      trier.ignoring(NumberFormatException.class).tryTo(run);
      verify(run, times(3)).run();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreVariousIgnoredException() throws InterruptedException {
      Runnable run = mock(Runnable.class);
      doThrow(NumberFormatException.class).doThrow(ArrayIndexOutOfBoundsException.class).doNothing().when(run).run();
      trier.ignoring(NumberFormatException.class, ArrayIndexOutOfBoundsException.class).tryTo(run);
      verify(run, times(3)).run();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldNotIgnoreIgnoredExceptionThrownAlways() {
      Runnable run = mock(Runnable.class);
      doThrow(NumberFormatException.class).when(run).run();
      assertThrows(LimitExceededException.class,
        () -> trier.ignoring(NumberFormatException.class).tryTo(run));
      verify(run, times(5)).run();
      assertThat(clock.now(), is(4L));
    }

    @Test
    void shouldNotIgnoreNotIgnoredException() throws InterruptedException {
      Runnable run = mock(Runnable.class);
      doThrow(NumberFormatException.class).doThrow(ArrayIndexOutOfBoundsException.class).doNothing().when(run).run();
      assertThrows(ArrayIndexOutOfBoundsException.class,
        () -> trier.ignoring(NumberFormatException.class).tryTo(run));
      verify(run, times(2)).run();
      assertThat(clock.now(), is(1L));
    }
  }

  @Nested
  class TryToRunSupplier {

    @Test
    void shouldReturnImmediatelyIfTheSupplierDoesNotThrow() throws InterruptedException {
      Supplier run = mock(Supplier.class);
      when(run.get()).thenReturn("OK");
      assertThat(trier.tryTo(run), is("OK"));
      verify(run, times(1)).get();
      assertThat(clock.now(), is(0L));
    }

    @Test
    void shouldIgnoreIgnoredExceptionThrownOnce() throws InterruptedException {
      Supplier run = mock(Supplier.class);
      when(run.get()).thenThrow(NumberFormatException.class).thenReturn("OK");
      assertThat(trier.ignoring(NumberFormatException.class).tryTo(run), is("OK"));
      verify(run, times(2)).get();
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldIgnoreIgnoredExceptionThrownSeveralTimes() throws InterruptedException {
      Supplier run = mock(Supplier.class);
      when(run.get()).thenThrow(NumberFormatException.class).thenThrow(NumberFormatException.class).thenReturn("OK");
      assertThat(trier.ignoring(NumberFormatException.class).tryTo(run), is("OK"));
      verify(run, times(3)).get();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreVariousIgnoredException() throws InterruptedException {
      Supplier run = mock(Supplier.class);
      when(run.get()).thenThrow(NumberFormatException.class).thenThrow(ArrayIndexOutOfBoundsException.class).thenReturn("OK");
      assertThat(trier.ignoring(NumberFormatException.class, ArrayIndexOutOfBoundsException.class).tryTo(run), is("OK"));
      verify(run, times(3)).get();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldNotIgnoreIgnoredExceptionThrownAlways() {
      Supplier run = mock(Supplier.class);
      when(run.get()).thenThrow(NumberFormatException.class);
      assertThrows(LimitExceededException.class,
        () -> trier.ignoring(NumberFormatException.class).tryTo(run));
      verify(run, times(5)).get();
      assertThat(clock.now(), is(4L));
    }

    @Test
    void shouldNotIgnoreNotIgnoredException() throws InterruptedException {
      Supplier run = mock(Supplier.class);
      when(run.get()).thenThrow(NumberFormatException.class).thenThrow(ArrayIndexOutOfBoundsException.class).thenReturn("OK");
      assertThrows(ArrayIndexOutOfBoundsException.class,
        () -> trier.ignoring(NumberFormatException.class).tryTo(run));
      verify(run, times(2)).get();
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldIgnoreIgnoredResultReturnedOnce() throws InterruptedException {
      Supplier run = mock(Supplier.class);
      when(run.get()).thenReturn("FAIL").thenReturn("OK");
      assertThat(trier.ignoring(res -> res.equals("FAIL")).tryTo(run), is("OK"));
      verify(run, times(2)).get();
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldIgnoreUnexpectedResultReturnedOnce() throws InterruptedException {
      Supplier run = mock(Supplier.class);
      when(run.get()).thenReturn("FAIL").thenReturn("OK");
      assertThat(trier.until(res -> res.equals("OK")).tryTo(run), is("OK"));
      verify(run, times(2)).get();
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldIgnoreIgnoredResultReturnedSeveralTimes() throws InterruptedException {
      Supplier run = mock(Supplier.class);
      when(run.get()).thenReturn("FAIL").thenReturn("FAIL").thenReturn("OK");
      assertThat(trier.ignoring(res -> res.equals("FAIL")).tryTo(run), is("OK"));
      verify(run, times(3)).get();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreUnexpectedResultReturnedSeveralTimes() throws InterruptedException {
      Supplier run = mock(Supplier.class);
      when(run.get()).thenReturn("FAIL1").thenReturn("FAIL2").thenReturn("OK");
      assertThat(trier.until(res -> res.equals("OK")).tryTo(run), is("OK"));
      verify(run, times(3)).get();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreBothIgnoredExceptionAndResult() throws InterruptedException {
      Supplier run = mock(Supplier.class);
      when(run.get()).thenThrow(NumberFormatException.class).thenReturn("FAIL").thenReturn("OK");
      assertThat(trier.until(res -> res.equals("OK")).ignoring(NumberFormatException.class).tryTo(run), is("OK"));
      verify(run, times(3)).get();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldNotIgnoreIgnoredResultReturnedAlways() {
      Supplier run = mock(Supplier.class);
      when(run.get()).thenReturn("FAIL");
      assertThrows(LimitExceededException.class,
        () -> trier.ignoring(res -> res.equals("FAIL")).tryTo(run));
      verify(run, times(5)).get();
      assertThat(clock.now(), is(4L));
    }

    @Test
    void shouldNotIgnoreUnexpectedResultReturnedAlways() {
      Supplier run = mock(Supplier.class);
      when(run.get()).thenReturn("FAIL");
      assertThrows(LimitExceededException.class,
        () -> trier.until(res -> res.equals("OK")).tryTo(run));
      verify(run, times(5)).get();
      assertThat(clock.now(), is(4L));
    }

  }

}
