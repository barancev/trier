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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenericsTrierTest {

  private TestingClock clock;
  private Trier<List<String>> trier;

  @Mock
  Supplier<List<String>> run;

  @BeforeEach
  void init() {
    clock = new TestingClock();
    trier = new CounterBasedTrier<>(5, clock, 1L);
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void shouldBeAbleToUseGenerics() throws InterruptedException {
    when(run.get()).thenReturn(new ArrayList<>()).thenReturn(Arrays.asList("OK"));
    List<String> result = trier.ignoring(List::isEmpty).tryTo(run);
    assertThat(result, is(Arrays.asList("OK")));
    verify(run, times(2)).get();
    assertThat(clock.now(), is(1L));
  }

  @Test
  void shouldBeAbleToUseGenerics2() throws InterruptedException {
    when(run.get()).thenReturn(new ArrayList<>()).thenReturn(Arrays.asList("OK"));
    List<String> result = CounterBasedTrier.times(5).tryTo(run);
    assertThat(result, is(Arrays.asList("OK")));
    verify(run, times(2)).get();
  }

}
