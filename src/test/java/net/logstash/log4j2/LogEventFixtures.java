package net.logstash.log4j2;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.spi.DefaultThreadContextStack;
import org.junit.Assert;

import static org.junit.Assert.*;

class LogEventFixtures {

  /**
   * @return a log event that uses all the bells and whistles, features, nooks and crannies
   */
  static Log4jLogEvent createLogEvent() {
    final Marker cMarker = MarkerManager.getMarker("Marker1");
    final Marker pMarker1 = MarkerManager.getMarker("ParentMarker1");
    final Marker pMarker2 = MarkerManager.getMarker("ParentMarker2");
    final Marker gfMarker = MarkerManager.getMarker("GrandFatherMarker");
    final Marker gmMarker = MarkerManager.getMarker("GrandMotherMarker");
    cMarker.addParents(pMarker1);
    cMarker.addParents(pMarker2);
    pMarker1.addParents(gmMarker);
    pMarker1.addParents(gfMarker);
    final Exception sourceHelper = new Exception();
    sourceHelper.fillInStackTrace();
    final Exception cause = new NullPointerException("testNPEx");
    sourceHelper.fillInStackTrace();
    final StackTraceElement source = sourceHelper.getStackTrace()[0];
    final IOException ioException = new IOException("testIOEx", cause);
    ioException.addSuppressed(new IndexOutOfBoundsException("I am suppressed exception 1"));
    ioException.addSuppressed(new IndexOutOfBoundsException("I am suppressed exception 2"));
    final ThrowableProxy throwableProxy = new ThrowableProxy(ioException);
    final Map<String, String> contextMap = new HashMap<String, String>();
    contextMap.put("MDC.A", "A_Value");
    contextMap.put("MDC.B", "B_Value");
    final DefaultThreadContextStack contextStack = new DefaultThreadContextStack(true);
    contextStack.clear();
    contextStack.push("stack_msg1");
    contextStack.add("stack_msg2");
    @SuppressWarnings("UnnecessaryLocalVariable")
    final Log4jLogEvent expected = Log4jLogEvent.newBuilder() //
        .setLoggerName("a.B") //
        .setMarker(cMarker) //
        .setLoggerFqcn("f.q.c.n") //
        .setLevel(Level.DEBUG) //
        .setMessage(new SimpleMessage("Msg")) //
        .setThrown(ioException) //
        .setThrownProxy(throwableProxy) //
        .setContextMap(contextMap) //
        .setContextStack(contextStack) //
        .setThreadName("MyThreadName") //
        .setSource(source) //
        .setTimeMillis(1).build();
    // validate event?
    return expected;
  }

  static Log4jLogEvent createSimpleLogEvent(boolean locationInfo) {
    final Map<String, String> contextMap = new HashMap<String, String>();
    contextMap.put("MDC.A", "A_Value");
    contextMap.put("MDC.B", "B_Value");
    final DefaultThreadContextStack contextStack = new DefaultThreadContextStack(true);
    contextStack.clear();
    contextStack.push("stack_msg1");
    contextStack.add("stack_msg2");
    Log4jLogEvent.Builder builder = Log4jLogEvent.newBuilder() //
        .setLoggerName("a.B") //
        .setLoggerFqcn("f.q.c.n") //
        .setLevel(Level.WARN) //
        .setMessage(new SimpleMessage("Msg")) //
        .setThrown(null) //
        .setThrownProxy(null) //
        .setContextMap(contextMap) //
        .setContextStack(contextStack) //
        .setThreadName("MyThreadName") //
        .setTimeMillis(1364844991207L);

    if (locationInfo) {
      final Exception sourceHelper = new Exception();
      sourceHelper.fillInStackTrace();
      final StackTraceElement source = sourceHelper.getStackTrace()[0];
      builder.setSource(source);
    }
    return builder.build();
  }

}
