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
package ru.stqa.repeater;

import org.junit.Before;
import org.junit.Test;

import static com.googlecode.catchexception.throwable.CatchThrowable.catchThrowable;
import static com.googlecode.catchexception.throwable.CatchThrowable.caughtThrowable;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class CounterBasedTrierTest {

  private TestingClock clock;
  private Trier trier;

  @Before
  public void init() {
    clock = new TestingClock();
    trier = new CounterBasedTrier(5, clock, 1L);
  }

  @Test
  public void shouldReturnImmediatelyIfTheRunnableDoesNotThrow() throws InterruptedException {
    Runnable run = mock(Runnable.class);
    trier.tryTo(run);
    verify(run, times(1)).run();
    assertThat(clock.now(), is(0L));
  }

  @Test
  public void shouldIgnoreIgnoredExceptionThrownOnce() throws InterruptedException {
    Runnable run = mock(Runnable.class);
    doThrow(NumberFormatException.class).doNothing().when(run).run();
    trier.ignoring(NumberFormatException.class).tryTo(run);
    verify(run, times(2)).run();
    assertThat(clock.now(), is(1L));
  }

  @Test
  public void shouldIgnoreIgnoredExceptionThrownSeveralTimes() throws InterruptedException {
    Runnable run = mock(Runnable.class);
    doThrow(NumberFormatException.class).doThrow(NumberFormatException.class).doNothing().when(run).run();
    trier.ignoring(NumberFormatException.class).tryTo(run);
    verify(run, times(3)).run();
    assertThat(clock.now(), is(2L));
  }

  @Test
  public void shouldIgnoreVariousIgnoredException() throws InterruptedException {
    Runnable run = mock(Runnable.class);
    doThrow(NumberFormatException.class).doThrow(ArrayIndexOutOfBoundsException.class).doNothing().when(run).run();
    trier.ignoring(NumberFormatException.class, ArrayIndexOutOfBoundsException.class).tryTo(run);
    verify(run, times(3)).run();
    assertThat(clock.now(), is(2L));
  }

  @Test
  public void shouldNotIgnoreIgnoredExceptionThrownAlways() {
    Runnable run = mock(Runnable.class);
    doThrow(NumberFormatException.class).when(run).run();
    catchThrowable(() -> trier.ignoring(NumberFormatException.class).tryTo(run));
    assertThat(caughtThrowable(), instanceOf(LimitExceededException.class));
    verify(run, times(5)).run();
    assertThat(clock.now(), is(4L));
  }

  @Test
  public void shouldNotIgnoreNotIgnoredException() throws InterruptedException {
    Runnable run = mock(Runnable.class);
    doThrow(NumberFormatException.class).doThrow(ArrayIndexOutOfBoundsException.class).doNothing().when(run).run();
    catchThrowable(() -> trier.ignoring(NumberFormatException.class).tryTo(run));
    assertThat(caughtThrowable(), instanceOf(ArrayIndexOutOfBoundsException.class));
    verify(run, times(2)).run();
    assertThat(clock.now(), is(1L));
  }

}
