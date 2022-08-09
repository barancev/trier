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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class TimeBasedTrier<X> extends Trier<X> {

  public static <T> TimeBasedTrier<T> during(long duration) {
    return new TimeBasedTrier<>(duration);
  }

  private final static long DEFAULT_SLEEP_TIMEOUT = 500;

  private final Clock clock;
  private final Sleeper sleeper;

  private final long duration;
  private final long interval;

  public TimeBasedTrier(long duration) {
    this(duration, new Clock() {}, new Sleeper() {}, DEFAULT_SLEEP_TIMEOUT);
  }

  public TimeBasedTrier(long duration, long interval) {
    this(duration, new Clock() {}, new Sleeper() {}, interval);
  }

  public TimeBasedTrier(long duration, Clock clock, Sleeper sleeper, long interval) {
    this.duration = duration;
    this.clock = checkNotNull(clock);
    this.sleeper = checkNotNull(sleeper);
    this.interval = checkNotNull(interval);
  }

  @Override
  public void tryTo(Runnable r) throws LimitExceededException, InterruptedException {
    long end = clock.laterBy(duration);
    Throwable lastException = null;
    while (true) {
      try {
        r.run();
        return;
      } catch (Throwable t) {
        if (! isExceptionIgnored(t)) {
          throw t;
        }
        lastException = t;
      }

      if (clock.past(end)) {
        String humanReadableTimeout = String.format("%1$tH:%1$tM:%1$tS.%1$tL", duration);
        String timeoutMessage = String.format(
          "Timed out after %s trying to perform action %s", humanReadableTimeout, r);
        throw new LimitExceededException(timeoutMessage, lastException);
      }

      sleeper.sleep(interval);
    }
  }

  @Override
  public <T extends X> T tryTo(Supplier<T> s) throws LimitExceededException, InterruptedException {
    long end = clock.laterBy(duration);
    Throwable lastException = null;
    while (true) {
      try {
        T res = s.get();
        if (! isResultIgnored(res)) {
          return res;
        }
      } catch (Throwable t) {
        if (! isExceptionIgnored(t)) {
          throw t;
        }
        lastException = t;
      }

      if (clock.past(end)) {
        String humanReadableTimeout = String.format("%1$tH:%1$tM:%1$tS.%1$tL", duration);
        String timeoutMessage = String.format(
          "Timed out after %s trying to perform action %s", humanReadableTimeout, s);
        throw new LimitExceededException(timeoutMessage, lastException);
      }

      sleeper.sleep(interval);
    }
  }

  @Override
  public <T> void tryTo(Consumer<T> c, T par) throws LimitExceededException, InterruptedException {
    long end = clock.laterBy(duration);
    Throwable lastException = null;
    while (true) {
      try {
        c.accept(par);
        return;
      } catch (Throwable t) {
        if (! isExceptionIgnored(t)) {
          throw t;
        }
        lastException = t;
      }

      if (clock.past(end)) {
        String humanReadableTimeout = String.format("%1$tH:%1$tM:%1$tS.%1$tL", duration);
        String timeoutMessage = String.format(
          "Timed out after %s trying to perform action %s", humanReadableTimeout, c);
        throw new LimitExceededException(timeoutMessage, lastException);
      }

      sleeper.sleep(interval);
    }
  }

  @Override
  public <T, R extends X> R tryTo(Function<T, R> f, T par) throws LimitExceededException, InterruptedException {
    long end = clock.laterBy(duration);
    Throwable lastException = null;
    while (true) {
      try {
        R res = f.apply(par);
        if (! isResultIgnored(res)) {
          return res;
        }
      } catch (Throwable t) {
        if (! isExceptionIgnored(t)) {
          throw t;
        }
        lastException = t;
      }

      if (clock.past(end)) {
        String humanReadableTimeout = String.format("%1$tH:%1$tM:%1$tS.%1$tL", duration);
        String timeoutMessage = String.format(
          "Timed out after %s trying to perform action %s", humanReadableTimeout, f);
        throw new LimitExceededException(timeoutMessage, lastException);
      }

      sleeper.sleep(interval);
    }
  }

}
