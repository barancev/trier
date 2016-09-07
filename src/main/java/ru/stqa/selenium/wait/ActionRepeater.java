/*
 * Copyright 2013 Alexei Barantsev
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
package ru.stqa.selenium.wait;

import com.google.common.base.Throwables;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Clock;
import org.openqa.selenium.support.ui.Duration;
import org.openqa.selenium.support.ui.Sleeper;
import org.openqa.selenium.support.ui.SystemClock;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Each ActionRepeater instance defines the maximum amount of time to attempt to perform an action,
 * as well as the frequency with which to call the action.
 *
 * <p>
 * Sample usage: <code>
 *   // Try to look for an element to be present on the page, checking<br>
 *   // for its presence once every 5 seconds until timeout of 30 seconds is expired.<br>
 *   ActionRepeater&lt;WebDriver&gt; repeater
 *     = new ActionRepeater&lt;WebDriver&gt;(driver, 10\/*seconds*\/, 1000\/*milliseconds*\/);<br>
 *   WebElement foo = repeater.tryTo(RepeatableActions.findElement(By.id("foo"));<br>
 * </code>
 *
 * <p>
 * <em>This class makes no thread safety guarantees.</em>
 *
 * @param <T> The action execution context type, usually {{@link WebDriver} or {{@link WebElement}}}.
 */
public class ActionRepeater <T> {

  public static ActionRepeater<WebDriver> with(WebDriver driver) {
    return new ActionRepeater<WebDriver>(driver);
  }

  public static ActionRepeater<WebDriver> with(WebDriver driver, long timeOutInSeconds) {
    return new ActionRepeater<WebDriver>(driver, timeOutInSeconds);
  }

  public static ActionRepeater<WebDriver> with(WebDriver driver, long timeOutInSeconds, long sleepInMillis) {
    return new ActionRepeater<WebDriver>(driver, timeOutInSeconds, sleepInMillis);
  }

  public static ActionRepeater<WebElement> with(WebElement element) {
    return new ActionRepeater<WebElement>(element);
  }

  public static ActionRepeater<WebElement> with(WebElement element, long timeOutInSeconds) {
    return new ActionRepeater<WebElement>(element, timeOutInSeconds);
  }

  public static ActionRepeater<WebElement> with(WebElement element, long timeOutInSeconds, long sleepInMillis) {
    return new ActionRepeater<WebElement>(element, timeOutInSeconds, sleepInMillis);
  }

  private final T context;
  private final Clock clock;
  private final Sleeper sleeper;

  private final Duration timeout;
  private final Duration interval;

  private final static long DEFAULT_SLEEP_TIMEOUT = 500;

  public ActionRepeater(T context) {
    this(context, new Duration(DEFAULT_SLEEP_TIMEOUT * 6, MILLISECONDS));
  }

  public ActionRepeater(T context, long timeOutInSeconds) {
    this(context, new Duration(timeOutInSeconds, SECONDS));
  }

  public ActionRepeater(T context, Duration timeOut) {
    this(context, timeOut, new Duration(DEFAULT_SLEEP_TIMEOUT, MILLISECONDS));
  }

  public ActionRepeater(T context, long timeOutInSeconds, long sleepInMillis) {
    this(context, new Duration(timeOutInSeconds, SECONDS), new Duration(sleepInMillis, MILLISECONDS));
  }

  public ActionRepeater(T context, Duration timeOut, Duration sleep) {
    this(context, new SystemClock(), Sleeper.SYSTEM_SLEEPER, timeOut, sleep);
  }

  /**
   * @param context The execution context to pass to the action
   * @param clock The clock to use when measuring the timeout
   * @param sleeper Object used to make the current thread go to sleep.
   * @param timeout Repetition timeout. An action should be repeated unless it succeeded ot the timeout passed.
   * @param sleep The interval between two attempts to perform an action.
   */
  protected ActionRepeater(T context, Clock clock, Sleeper sleeper, Duration timeout, Duration sleep) {
    this.context = checkNotNull(context);
    this.clock = checkNotNull(clock);
    this.sleeper = checkNotNull(sleeper);
    this.timeout = checkNotNull(timeout);
    this.interval = checkNotNull(sleep);
  }

  /**
   * Repeatedly attempts to perform the action until one of the following occurs:
   * <ol>
   * <li>the function returns a value that should not be ignored,</li>
   * <li>the function throws a exception that should not be ignored,</li>
   * <li>the timeout expires,
   * <li>
   * <li>the current thread is interrupted</li>
   * </ol>
   *
   * @param action the action to be performed repeatedly
   * @param <V> The action's expected return type.
   * @return The action' return value if the action returned a result that should not be ignored.
   * @throws org.openqa.selenium.TimeoutException If the timeout expires.
   */
  public <V> V tryTo(RepeatableAction<? super T, V> action) {
    long end = clock.laterBy(timeout.in(MILLISECONDS));
    Throwable lastException = null;
    while (true) {
      try {
        V result = action.apply(context);
        if (! action.shouldIgnoreResult(result)) {
          return result;
        }
      } catch (Throwable e) {
        if (! action.shouldIgnoreException(e)) {
          throw Throwables.propagate(e);
        } else {
          lastException = e;
        }
      }

      // Check the timeout after evaluating the function to ensure conditions
      // with a zero timeout can succeed.
      if (!clock.isNowBefore(end)) {
        String timeoutMessage = String.format(
          "Timed out after %d seconds trying to perform action %s",
          timeout.in(SECONDS), action);
        throw new TimeoutException(timeoutMessage, lastException);
      }

      try {
        sleeper.sleep(interval);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new WebDriverException(e);
      }
    }
  }
}
