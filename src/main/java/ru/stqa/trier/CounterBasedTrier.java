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

import com.google.common.annotations.VisibleForTesting;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public class CounterBasedTrier extends Trier {

  public static CounterBasedTrier times(int n) {
    return new CounterBasedTrier(n);
  }

  private final static long DEFAULT_SLEEP_TIMEOUT = 500;

  private final Sleeper sleeper;

  private final long interval;

  private final int n;

  public CounterBasedTrier(int n) {
    this(n, new Sleeper() {}, DEFAULT_SLEEP_TIMEOUT);
  }

  @VisibleForTesting
  CounterBasedTrier(int n, Sleeper sleeper, long interval) {
    this.n = n;
    this.sleeper = checkNotNull(sleeper);
    this.interval = checkNotNull(interval);
  }

  @Override
  public void tryTo(Runnable r) throws InterruptedException {
    for (int i = 0; i < n; i++) {
      try {
        r.run();
        return;
      } catch (Throwable t) {
        if (! isExceptionIgnored(t)) {
          throw t;
        }
      }
      if (i < n-1) {
        sleeper.sleep(interval);
      }
    }
    throw new LimitExceededException();
  }

  @Override
  public <T> T tryTo(Supplier<T> s) throws InterruptedException {
    for (int i = 0; i < n; i++) {
      try {
        T res = s.get();
        if (! isResultIgnored(res)) {
          return res;
        }
      } catch (Throwable t) {
        if (! isExceptionIgnored(t)) {
          throw t;
        }
      }
      if (i < n-1) {
        sleeper.sleep(interval);
      }
    }
    throw new LimitExceededException();
  }

  @Override
  public <T> void tryTo(Consumer<T> c, T par) throws InterruptedException {
    for (int i = 0; i < n; i++) {
      try {
        c.accept(par);
      } catch (Throwable t) {
        if (! isExceptionIgnored(t)) {
          throw t;
        }
      }
      if (i < n-1) {
        sleeper.sleep(interval);
      }
    }
    throw new LimitExceededException();
  }

  @Override
  public <T, R> R tryTo(Function<T, R> f, T par) throws InterruptedException {
    for (int i = 0; i < n; i++) {
      try {
        R res = f.apply(par);
        if (! isResultIgnored(res)) {
          return res;
        }
      } catch (Throwable t) {
        if (! isExceptionIgnored(t)) {
          throw t;
        }
      }
      if (i < n-1) {
        sleeper.sleep(interval);
      }
    }
    throw new LimitExceededException();
  }

}
