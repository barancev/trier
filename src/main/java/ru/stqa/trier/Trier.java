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

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class Trier<X> {

  private Class<? extends Throwable>[] ignoredExceptions;
  private Predicate<X> ignoredResult;

  abstract public void tryTo(Runnable r) throws InterruptedException;
  abstract public <T extends X> T tryTo(Supplier<T> s) throws InterruptedException;
  abstract public <T> void tryTo(Consumer<T> c, T par) throws InterruptedException;
  abstract public <T, R extends X> R tryTo(Function<T, R> f, T par) throws InterruptedException;

  @SafeVarargs
  final public Trier ignoring(Class<? extends Throwable>... ignoredExceptions) {
    if (this.ignoredExceptions != null) {
      throw new IllegalStateException("Ignored exceptions can be set once only");
    }
    this.ignoredExceptions = ignoredExceptions;
    return this;
  }

  final public Trier ignoring(Predicate<X> ignoredResult) {
    if (this.ignoredResult != null) {
      throw new IllegalStateException("Predicate to ignore unwanted results can be set once only");
    }
    this.ignoredResult = ignoredResult;
    return this;
  }

  final public Trier until(Predicate<X> expectedResult) {
    if (this.ignoredResult != null) {
      throw new IllegalStateException("Predicate to ignore unwanted results can be set once only");
    }
    this.ignoredResult = expectedResult.negate();
    return this;
  }

  final protected boolean isExceptionIgnored(Throwable t) {
    if (ignoredExceptions != null) {
      for (Class<? extends Throwable> cls : ignoredExceptions) {
        if (cls.isAssignableFrom(t.getClass())) {
          return true;
        }
      }
      return false;

    } else {
      // By default ignore all exceptions
      return true;
    }
  }

  final protected <T> boolean isResultIgnored(X result) {
    if (ignoredResult != null) {
      return ignoredResult.test(result);

    } else {
      // by default ignore null object values, false boolean values, zero numbers, empty strings and empty collections
      if (result == null) {
        return true;
      }
      if (result instanceof Number && ((Number) result).doubleValue() == 0.0) {
        return true;
      }
      if (result.equals(false)) {
        return true;
      }
      if (result.equals("")) {
        return true;
      }
      if (result instanceof Collection && ((Collection) result).isEmpty()) {
        return true;
      }
      return false;
    }
  }

}
