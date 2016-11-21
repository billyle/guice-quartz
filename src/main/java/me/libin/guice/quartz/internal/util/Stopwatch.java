package me.libin.guice.quartz.internal.util;

import java.util.logging.Logger;

/**
 * Enables simple performance monitoring.
 *
 * @author crazybob@google.com (Bob Lee)
 */
public final class Stopwatch {
  private static final Logger logger = Logger.getLogger(Stopwatch.class.getName());

  private long start = System.currentTimeMillis();

  /**
   * Resets and returns elapsed time in milliseconds.
   */
  public long reset() {
    long now = System.currentTimeMillis();
    try {
      return now - start;
    } finally {
      start = now;
    }
  }

  /**
   * Resets and logs elapsed time in milliseconds.
   */
  public void resetAndLog(String label) {
    logger.fine(label + ": " + reset() + "ms");
  }
}
