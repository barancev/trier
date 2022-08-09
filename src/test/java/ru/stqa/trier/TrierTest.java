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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrierTest {

  private TestingClock clock;
  private Trier trier;

  @BeforeEach
  void init() {
    clock = new TestingClock();
    //trier = new CounterBasedTrier(5, clock, 1L);
    trier = new TimeBasedTrier(4L, clock, clock, 1L);
  }

  @Nested
  class TryToRunRunnable {

    @Mock
    Runnable run;

    @BeforeEach
    void initMocks() {
      MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldReturnImmediatelyIfTheRunnableDoesNotThrow() throws LimitExceededException, InterruptedException {
      trier.tryTo(run);
      verify(run, times(1)).run();
      assertThat(clock.now(), is(0L));
    }

    @Test
    void shouldIgnoreAllExceptionsByDefault() throws LimitExceededException, InterruptedException {
      doThrow(NumberFormatException.class).doThrow(ArrayIndexOutOfBoundsException.class).doNothing().when(run).run();
      trier.tryTo(run);
      verify(run, times(3)).run();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreIgnoredExceptionThrownOnce() throws LimitExceededException, InterruptedException {
      doThrow(NumberFormatException.class).doNothing().when(run).run();
      trier.ignoring(NumberFormatException.class).tryTo(run);
      verify(run, times(2)).run();
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldIgnoreIgnoredExceptionThrownSeveralTimes() throws LimitExceededException, InterruptedException {
      doThrow(NumberFormatException.class).doThrow(NumberFormatException.class).doNothing().when(run).run();
      trier.ignoring(NumberFormatException.class).tryTo(run);
      verify(run, times(3)).run();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreVariousIgnoredException() throws LimitExceededException, InterruptedException {
      doThrow(NumberFormatException.class).doThrow(ArrayIndexOutOfBoundsException.class).doNothing().when(run).run();
      trier.ignoring(NumberFormatException.class, ArrayIndexOutOfBoundsException.class).tryTo(run);
      verify(run, times(3)).run();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldNotIgnoreIgnoredExceptionThrownAlways() {
      doThrow(NumberFormatException.class).when(run).run();
      Throwable thrown = assertThrows(LimitExceededException.class,
        () -> trier.ignoring(NumberFormatException.class).tryTo(run));
      assertThat(thrown.getCause(), instanceOf(NumberFormatException.class));
      verify(run, times(5)).run();
      assertThat(clock.now(), is(4L));
    }

    @Test
    void shouldRememberLastThrownException() {
      doThrow(ArrayIndexOutOfBoundsException.class).doThrow(NumberFormatException.class).when(run).run();
      Throwable thrown = assertThrows(LimitExceededException.class,
        () -> trier.ignoring(NumberFormatException.class, ArrayIndexOutOfBoundsException.class).tryTo(run));
      assertThat(thrown.getCause(), instanceOf(NumberFormatException.class));
      verify(run, times(5)).run();
      assertThat(clock.now(), is(4L));
    }

    @Test
    void shouldNotIgnoreNotIgnoredException() throws InterruptedException {
      doThrow(NumberFormatException.class).doThrow(ArrayIndexOutOfBoundsException.class).doNothing().when(run).run();
      assertThrows(ArrayIndexOutOfBoundsException.class,
        () -> trier.ignoring(NumberFormatException.class).tryTo(run));
      verify(run, times(2)).run();
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldPrintTimeoutInHumanReadableFormat() {
      doThrow(ArrayIndexOutOfBoundsException.class).doThrow(NumberFormatException.class).when(run).run();
      Throwable thrown = assertThrows(LimitExceededException.class,
        () -> trier.ignoring(NumberFormatException.class, ArrayIndexOutOfBoundsException.class).tryTo(run));
      assertThat(thrown.getMessage(), equalTo("Timed out after 01:00:00.004 trying to perform action run"));
    }
  }

  @Nested
  class TryToRunSupplier {

    @Mock
    Supplier<Object> run;

    @BeforeEach
    void initMocks() {
      MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldReturnImmediatelyIfTheSupplierDoesNotThrow() throws LimitExceededException, InterruptedException {
      when(run.get()).thenReturn("OK");
      assertThat(trier.tryTo(run), is("OK"));
      verify(run, times(1)).get();
      assertThat(clock.now(), is(0L));
    }

    @Test
    void shouldIgnoreAllExceptionsByDefault() throws LimitExceededException, InterruptedException {
      when(run.get()).thenThrow(NumberFormatException.class).thenThrow(NumberFormatException.class).thenReturn("OK");
      assertThat(trier.tryTo(run), is("OK"));
      verify(run, times(3)).get();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreNullFalseEmptyStringAndEmptyCollectionByDefault() throws LimitExceededException, InterruptedException {
      when(run.get()).thenReturn(null).thenReturn("").thenReturn(false).thenReturn(new ArrayList<String>()).thenReturn("OK");
      assertThat(trier.tryTo(run), is("OK"));
      verify(run, times(5)).get();
      assertThat(clock.now(), is(4L));
    }

    @Test
    void shouldIgnoreZeroesByDefault() throws LimitExceededException, InterruptedException {
      when(run.get()).thenReturn(0).thenReturn(0L).thenReturn(0.0).thenReturn("OK");
      assertThat(trier.tryTo(run), is("OK"));
      verify(run, times(4)).get();
      assertThat(clock.now(), is(3L));
    }

    @Test
    void shouldIgnoreIgnoredExceptionThrownOnce() throws LimitExceededException, InterruptedException {
      when(run.get()).thenThrow(NumberFormatException.class).thenReturn("OK");
      assertThat(trier.ignoring(NumberFormatException.class).tryTo(run), is("OK"));
      verify(run, times(2)).get();
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldIgnoreIgnoredExceptionThrownSeveralTimes() throws LimitExceededException, InterruptedException {
      when(run.get()).thenThrow(NumberFormatException.class).thenThrow(NumberFormatException.class).thenReturn("OK");
      assertThat(trier.ignoring(NumberFormatException.class).tryTo(run), is("OK"));
      verify(run, times(3)).get();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreVariousIgnoredException() throws LimitExceededException, InterruptedException {
      when(run.get()).thenThrow(NumberFormatException.class).thenThrow(ArrayIndexOutOfBoundsException.class).thenReturn("OK");
      assertThat(trier.ignoring(NumberFormatException.class, ArrayIndexOutOfBoundsException.class).tryTo(run), is("OK"));
      verify(run, times(3)).get();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldNotIgnoreIgnoredExceptionThrownAlways() {
      when(run.get()).thenThrow(NumberFormatException.class);
      Throwable thrown = assertThrows(LimitExceededException.class,
        () -> trier.ignoring(NumberFormatException.class).tryTo(run));
      assertThat(thrown.getCause(), instanceOf(NumberFormatException.class));
      verify(run, times(5)).get();
      assertThat(clock.now(), is(4L));
    }

    @Test
    void shouldNotIgnoreNotIgnoredException() throws InterruptedException {
      when(run.get()).thenThrow(NumberFormatException.class).thenThrow(ArrayIndexOutOfBoundsException.class).thenReturn("OK");
      assertThrows(ArrayIndexOutOfBoundsException.class,
        () -> trier.ignoring(NumberFormatException.class).tryTo(run));
      verify(run, times(2)).get();
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldIgnoreIgnoredResultReturnedOnce() throws LimitExceededException, InterruptedException {
      when(run.get()).thenReturn("FAIL").thenReturn("OK");
      assertThat(trier.ignoring(res -> res.equals("FAIL")).tryTo(run), is("OK"));
      verify(run, times(2)).get();
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldIgnoreUnexpectedResultReturnedOnce() throws LimitExceededException, InterruptedException {
      when(run.get()).thenReturn("FAIL").thenReturn("OK");
      assertThat(trier.until(res -> res.equals("OK")).tryTo(run), is("OK"));
      verify(run, times(2)).get();
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldIgnoreIgnoredResultReturnedSeveralTimes() throws LimitExceededException, InterruptedException {
      when(run.get()).thenReturn("FAIL").thenReturn("FAIL").thenReturn("OK");
      assertThat(trier.ignoring(res -> res.equals("FAIL")).tryTo(run), is("OK"));
      verify(run, times(3)).get();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreUnexpectedResultReturnedSeveralTimes() throws LimitExceededException, InterruptedException {
      when(run.get()).thenReturn("FAIL1").thenReturn("FAIL2").thenReturn("OK");
      assertThat(trier.until(res -> res.equals("OK")).tryTo(run), is("OK"));
      verify(run, times(3)).get();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreBothIgnoredExceptionAndResult() throws LimitExceededException, InterruptedException {
      when(run.get()).thenThrow(NumberFormatException.class).thenReturn("FAIL").thenReturn("OK");
      assertThat(trier.until(res -> res.equals("OK")).ignoring(NumberFormatException.class).tryTo(run), is("OK"));
      verify(run, times(3)).get();
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldNotIgnoreIgnoredResultReturnedAlways() {
      when(run.get()).thenReturn("FAIL");
      Throwable thrown = assertThrows(LimitExceededException.class,
        () -> trier.ignoring(res -> res.equals("FAIL")).tryTo(run));
      assertThat(thrown.getCause(), nullValue());
      verify(run, times(5)).get();
      assertThat(clock.now(), is(4L));
    }

    @Test
    void shouldNotIgnoreUnexpectedResultReturnedAlways() {
      when(run.get()).thenReturn("FAIL");
      Throwable thrown = assertThrows(LimitExceededException.class,
        () -> trier.until(res -> res.equals("OK")).tryTo(run));
      assertThat(thrown.getCause(), nullValue());
      verify(run, times(5)).get();
      assertThat(clock.now(), is(4L));
    }

    @Test
    void shouldPrintTimeoutInHumanReadableFormat() {
      when(run.get()).thenThrow(NumberFormatException.class);
      Throwable thrown = assertThrows(LimitExceededException.class,
        () -> trier.ignoring(NumberFormatException.class).tryTo(run));
      assertThat(thrown.getMessage(), equalTo("Timed out after 01:00:00.004 trying to perform action run"));
    }
  }

  @Nested
  class TryToRunConsumer {

    @Mock
    Consumer<String> run;

    @BeforeEach
    void initMocks() {
      MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldReturnImmediatelyIfTheConsumerDoesNotThrow() throws LimitExceededException, InterruptedException {
      trier.tryTo(run, "IN");
      verify(run, times(1)).accept("IN");
      assertThat(clock.now(), is(0L));
    }

    @Test
    void shouldIgnoreAllExceptionsByDefault() throws LimitExceededException, InterruptedException {
      doThrow(NumberFormatException.class).doThrow(ArrayIndexOutOfBoundsException.class).doNothing().when(run).accept("IN");
      trier.tryTo(run, "IN");
      verify(run, times(3)).accept("IN");
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreIgnoredExceptionThrownOnce() throws LimitExceededException, InterruptedException {
      doThrow(NumberFormatException.class).doNothing().when(run).accept("IN");
      trier.ignoring(NumberFormatException.class).tryTo(run, "IN");
      verify(run, times(2)).accept("IN");
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldIgnoreIgnoredExceptionThrownSeveralTimes() throws LimitExceededException, InterruptedException {
      doThrow(NumberFormatException.class).doThrow(NumberFormatException.class).doNothing().when(run).accept("IN");
      trier.ignoring(NumberFormatException.class).tryTo(run, "IN");
      verify(run, times(3)).accept("IN");
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreVariousIgnoredException() throws LimitExceededException, InterruptedException {
      doThrow(NumberFormatException.class).doThrow(ArrayIndexOutOfBoundsException.class).doNothing().when(run).accept("IN");
      trier.ignoring(NumberFormatException.class, ArrayIndexOutOfBoundsException.class).tryTo(run, "IN");
      verify(run, times(3)).accept("IN");
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldNotIgnoreIgnoredExceptionThrownAlways() {
      doThrow(NumberFormatException.class).when(run).accept("IN");
      Throwable thrown = assertThrows(LimitExceededException.class,
        () -> trier.ignoring(NumberFormatException.class).tryTo(run, "IN"));
      assertThat(thrown.getCause(), instanceOf(NumberFormatException.class));
      verify(run, times(5)).accept("IN");
      assertThat(clock.now(), is(4L));
    }

    @Test
    void shouldNotIgnoreNotIgnoredException() throws InterruptedException {
      doThrow(NumberFormatException.class).doThrow(ArrayIndexOutOfBoundsException.class).doNothing().when(run).accept("IN");
      assertThrows(ArrayIndexOutOfBoundsException.class,
        () -> trier.ignoring(NumberFormatException.class).tryTo(run, "IN"));
      verify(run, times(2)).accept("IN");
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldReturnTimeoutMessageInHumanReadableFormat() {
      doThrow(NumberFormatException.class).when(run).accept("IN");
      Throwable thrown = assertThrows(LimitExceededException.class,
        () -> trier.ignoring(NumberFormatException.class).tryTo(run, "IN"));
      assertThat(thrown.getMessage(), equalTo("Timed out after 01:00:00.004 trying to perform action run"));
    }
  }

  @Nested
  class TryToRunFunction {

    @Mock
    Function<String, Object> run;

    @BeforeEach
    void initMocks() {
      MockitoAnnotations.initMocks(this);
    }

    @Test
    void shouldReturnImmediatelyIfTheFunctionDoesNotThrow() throws LimitExceededException, InterruptedException {
      when(run.apply("IN")).thenReturn("OK");
      assertThat(trier.tryTo(run, "IN"), is("OK"));
      verify(run, times(1)).apply("IN");
      assertThat(clock.now(), is(0L));
    }

    @Test
    void shouldIgnoreAllExceptionsByDefault() throws LimitExceededException, InterruptedException {
      when(run.apply("IN")).thenThrow(NumberFormatException.class).thenThrow(NumberFormatException.class).thenReturn("OK");
      assertThat(trier.tryTo(run, "IN"), is("OK"));
      verify(run, times(3)).apply("IN");
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreNullFalseEmptyStringAndEmptyCollectionByDefault() throws LimitExceededException, InterruptedException {
      when(run.apply("IN")).thenReturn(null).thenReturn("").thenReturn(false).thenReturn(new ArrayList<String>()).thenReturn("OK");
      assertThat(trier.tryTo(run, "IN"), is("OK"));
      verify(run, times(5)).apply("IN");
      assertThat(clock.now(), is(4L));
    }

    @Test
    void shouldIgnoreZeroesByDefault() throws LimitExceededException, InterruptedException {
      when(run.apply("IN")).thenReturn(0).thenReturn(0L).thenReturn(0.0).thenReturn("OK");
      assertThat(trier.tryTo(run, "IN"), is("OK"));
      verify(run, times(4)).apply("IN");
      assertThat(clock.now(), is(3L));
    }

    @Test
    void shouldIgnoreIgnoredExceptionThrownOnce() throws LimitExceededException, InterruptedException {
      when(run.apply("IN")).thenThrow(NumberFormatException.class).thenReturn("OK");
      assertThat(trier.ignoring(NumberFormatException.class).tryTo(run, "IN"), is("OK"));
      verify(run, times(2)).apply("IN");
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldIgnoreIgnoredExceptionThrownSeveralTimes() throws LimitExceededException, InterruptedException {
      when(run.apply("IN")).thenThrow(NumberFormatException.class).thenThrow(NumberFormatException.class).thenReturn("OK");
      assertThat(trier.ignoring(NumberFormatException.class).tryTo(run, "IN"), is("OK"));
      verify(run, times(3)).apply("IN");
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreVariousIgnoredException() throws LimitExceededException, InterruptedException {
      when(run.apply("IN")).thenThrow(NumberFormatException.class).thenThrow(ArrayIndexOutOfBoundsException.class).thenReturn("OK");
      assertThat(trier.ignoring(NumberFormatException.class, ArrayIndexOutOfBoundsException.class).tryTo(run, "IN"), is("OK"));
      verify(run, times(3)).apply("IN");
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldNotIgnoreIgnoredExceptionThrownAlways() {
      when(run.apply("IN")).thenThrow(NumberFormatException.class);
      Throwable thrown = assertThrows(LimitExceededException.class,
        () -> trier.ignoring(NumberFormatException.class).tryTo(run, "IN"));
      assertThat(thrown.getCause(), instanceOf(NumberFormatException.class));
      verify(run, times(5)).apply("IN");
      assertThat(clock.now(), is(4L));
    }

    @Test
    void shouldNotIgnoreNotIgnoredException() throws InterruptedException {
      when(run.apply("IN")).thenThrow(NumberFormatException.class).thenThrow(ArrayIndexOutOfBoundsException.class).thenReturn("OK");
      assertThrows(ArrayIndexOutOfBoundsException.class,
        () -> trier.ignoring(NumberFormatException.class).tryTo(run, "IN"));
      verify(run, times(2)).apply("IN");
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldIgnoreIgnoredResultReturnedOnce() throws LimitExceededException, InterruptedException {
      when(run.apply("IN")).thenReturn("FAIL").thenReturn("OK");
      assertThat(trier.ignoring(res -> res.equals("FAIL")).tryTo(run, "IN"), is("OK"));
      verify(run, times(2)).apply("IN");
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldIgnoreUnexpectedResultReturnedOnce() throws LimitExceededException, InterruptedException {
      when(run.apply("IN")).thenReturn("FAIL").thenReturn("OK");
      assertThat(trier.until(res -> res.equals("OK")).tryTo(run, "IN"), is("OK"));
      verify(run, times(2)).apply("IN");
      assertThat(clock.now(), is(1L));
    }

    @Test
    void shouldIgnoreIgnoredResultReturnedSeveralTimes() throws LimitExceededException, InterruptedException {
      when(run.apply("IN")).thenReturn("FAIL").thenReturn("FAIL").thenReturn("OK");
      assertThat(trier.ignoring(res -> res.equals("FAIL")).tryTo(run, "IN"), is("OK"));
      verify(run, times(3)).apply("IN");
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreUnexpectedResultReturnedSeveralTimes() throws LimitExceededException, InterruptedException {
      when(run.apply("IN")).thenReturn("FAIL1").thenReturn("FAIL2").thenReturn("OK");
      assertThat(trier.until(res -> res.equals("OK")).tryTo(run, "IN"), is("OK"));
      verify(run, times(3)).apply("IN");
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldIgnoreBothIgnoredExceptionAndResult() throws LimitExceededException, InterruptedException {
      when(run.apply("IN")).thenThrow(NumberFormatException.class).thenReturn("FAIL").thenReturn("OK");
      assertThat(trier.until(res -> res.equals("OK")).ignoring(NumberFormatException.class).tryTo(run, "IN"), is("OK"));
      verify(run, times(3)).apply("IN");
      assertThat(clock.now(), is(2L));
    }

    @Test
    void shouldNotIgnoreIgnoredResultReturnedAlways() {
      when(run.apply("IN")).thenReturn("FAIL");
      Throwable thrown = assertThrows(LimitExceededException.class,
        () -> trier.ignoring(res -> res.equals("FAIL")).tryTo(run, "IN"));
      assertThat(thrown.getCause(), nullValue());
      verify(run, times(5)).apply("IN");
      assertThat(clock.now(), is(4L));
    }

    @Test
    void shouldNotIgnoreUnexpectedResultReturnedAlways() {
      when(run.apply("IN")).thenReturn("FAIL");
      Throwable thrown = assertThrows(LimitExceededException.class,
        () -> trier.until(res -> res.equals("OK")).tryTo(run, "IN"));
      assertThat(thrown.getCause(), nullValue());
      verify(run, times(5)).apply("IN");
      assertThat(clock.now(), is(4L));
    }

    @Test
    void shouldReturnTimeoutMessageInHumanReadableFormat() {
      when(run.apply("IN")).thenThrow(NumberFormatException.class);
      Throwable thrown = assertThrows(LimitExceededException.class,
        () -> trier.ignoring(NumberFormatException.class).tryTo(run, "IN"));
      assertThat(thrown.getMessage(), equalTo("Timed out after 01:00:00.004 trying to perform action run"));
    }
  }
}
