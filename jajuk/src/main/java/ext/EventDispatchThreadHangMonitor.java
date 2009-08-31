/**
 * LGPL
 * http://elliotth.blogspot.com/2005/05/automatically-detecting-awt-event.html
 */
package ext;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.util.Timer;
import java.util.TimerTask;

import org.jajuk.util.log.Log;

/**
 * Monitors the AWT event dispatch thread for events that take longer than a
 * certain time to be dispatched.
 * 
 * The principle is to record the time at which we start processing an event,
 * and have another thread check frequently to see if we're still processing. If
 * the other thread notices that we've been processing a single event for too
 * long, it prints a stack trace showing what the event dispatch thread is
 * doing, and continues to time it until it finally finishes.
 * 
 * This is useful in determining what code is causing your Java application's
 * GUI to be unresponsive.
 * 
 * @author Elliott Hughes <enh@jessies.org>
 */
public final class EventDispatchThreadHangMonitor extends EventQueue {
  private static final EventQueue INSTANCE = new EventDispatchThreadHangMonitor();

  // Time to wait between checks that the event dispatch thread isn't hung.
  private static final long CHECK_INTERVAL_MS = 1000;

  // Maximum time we won't warn about in test mode
  private static final long UNREASONABLE_DISPATCH_DURATION_MS_TEST = 1000;

  // Used as the value of startedLastEventDispatchAt when we're not in
  // the middle of event dispatch.
  private static final long NO_CURRENT_EVENT = 0;

  // When we started dispatching the current event, in milliseconds.
  private long startedLastEventDispatchAt = NO_CURRENT_EVENT;

  // Have we already dumped a stack trace for the current event dispatch?
  private boolean reportedHang = false;

  // The event dispatch thread, for the purpose of getting stack traces.
  private Thread eventDispatchThread = null;

  private EventDispatchThreadHangMonitor() {
    super();

    initTimer();
  }

  /**
   * Sets up a timer to check for hangs frequently.
   */
  private void initTimer() {
    final long initialDelayMs = 0;
    final boolean isDaemon = true;
    Timer timer = new Timer("EventDispatchThreadHangMonitor", isDaemon);
    timer.schedule(new HangChecker(), initialDelayMs, CHECK_INTERVAL_MS);
  }

  private class HangChecker extends TimerTask {
    @Override
    public void run() {
      // Synchronize on the outer class, because that's where all
      // the state lives.
      synchronized (INSTANCE) {
        checkForHang();
      }
    }

    private void checkForHang() {
      if (startedLastEventDispatchAt == NO_CURRENT_EVENT) {
        // We don't destroy the timer when there's nothing happening
        // because it would mean a lot more work on every single AWT
        // event that gets dispatched.
        return;
      }
      // check if elapsed time is not exceed (used only in test mode)
      if (timeSoFar() > UNREASONABLE_DISPATCH_DURATION_MS_TEST) {
        reportHang();
      }
    }

    private void reportHang() {
      if (reportedHang) {
        // Don't keep reporting the same hang every 100 ms.
        return;
      }
      reportedHang = true;
      Log.debug("--- event dispatch thread stuck processing event for " + timeSoFar() + " ms:");
      StackTraceElement[] stackTrace = eventDispatchThread.getStackTrace();
      printStackTrace(stackTrace);
    }

    private void printStackTrace(StackTraceElement[] stackTrace) {
      // We know that it's not interesting to show any code above where
      // we get involved in event dispatch, so we stop printing the stack
      // trace when we get as far back as our code.
      final String ourEventQueueClassName = EventDispatchThreadHangMonitor.class.getName();
      for (StackTraceElement stackTraceElement : stackTrace) {
        if (stackTraceElement.getClassName().equals(ourEventQueueClassName)) {
          return;
        }
        Log.debug("    " + stackTraceElement);
      }
    }
  }

  /**
   * Returns how long we've been processing the current event (in milliseconds).
   */
  private long timeSoFar() {
    long currentTime = System.currentTimeMillis();
    return (currentTime - startedLastEventDispatchAt);
  }

  /**
   * Sets up hang detection for the event dispatch thread.
   */
  public static void initMonitoring() {
    Toolkit.getDefaultToolkit().getSystemEventQueue().push(INSTANCE);
  }

  /**
   * Overrides EventQueue.dispatchEvent to call our pre and post hooks either
   * side of the system's event dispatch code.
   */
  @Override
  protected void dispatchEvent(AWTEvent event) {
    try {
      preDispatchEvent();
      super.dispatchEvent(event);
      postDispatchEvent();
    } catch (Throwable t) {
      Log.error(t);
    }
  }

  /**
   * Stores the time at which we started processing the current event.
   */
  private synchronized void preDispatchEvent() {
    if (eventDispatchThread == null) {
      // I don't know of any API for getting the event dispatch thread,
      // but we can assume that it's the current thread if we're in the
      // middle of dispatching an AWT event...
      eventDispatchThread = Thread.currentThread();
    }

    reportedHang = false;
    startedLastEventDispatchAt = System.currentTimeMillis();
  }

  /**
   * Reports the end of any ongoing hang, and notes that we're no longer
   * processing an event.
   */
  private synchronized void postDispatchEvent() {
    if (reportedHang) {
      Log.debug("--- event dispatch thread unstuck after " + timeSoFar() + " ms.");
    }
    startedLastEventDispatchAt = NO_CURRENT_EVENT;
  }
}
