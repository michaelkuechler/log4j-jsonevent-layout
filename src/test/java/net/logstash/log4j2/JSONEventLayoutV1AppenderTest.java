package net.logstash.log4j2;


import junit.framework.Assert;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class JSONEventLayoutV1AppenderTest {

  public static final String PATH_LOG_FILE    = "test-with-locationInfo.log";
  public static final String PATH_CONFIG_FILE = "FileAppender-JSONEventLayout.xml";

  private boolean locationInfo = true;

  @Before
  public void setLoggerConfig() {
    System.setProperty("org.apache.logging.log4j.level", "DEBUG");
    System.setProperty("log4j.configurationFile", PATH_CONFIG_FILE);
    System.setProperty("log4j.locationInfo", "true");
  }

  @After
  public void cleanUp() {
    ThreadContext.clearAll();
    System.clearProperty("org.apache.logging.log4j.level");
    System.clearProperty("log4j.configurationFile");
    FileUtils.deleteQuietly(new File(PATH_LOG_FILE));

  }


  @Test
  public void testAllLogEventsWithLocationInfo() throws IOException {
    testAllLogEvents();
  }

  @Test
  public void testAllLogEventsWithoutLocationInfo() throws IOException {
    locationInfo = false;
    System.setProperty("log4j.locationInfo", "false");
    final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    ctx.stop();
    ctx.start();
    testAllLogEvents();
  }


  public void testAllLogEvents() throws IOException {
    new LoggerHelper().logSomeEvents();
    List<String> lines = FileUtils.readLines(new File(PATH_LOG_FILE), Charset.forName("UTF-8"));

    for (String line : lines) {
      assertTrue("Should be valid JSON", JSONValue.isValidJsonStrict(line));
      JSONObject json = (JSONObject) JSONValue.parse(line);

      if (locationInfo) {
        assertProperty(json, "file", "LoggerHelper.java");
        assertProperty(json, "method", "logSomeEvents");
      } else {
        assertProperty(json, "file", null);
        assertProperty(json, "method", null);
      }


      assertProperty(json, "thread_name", "main");
      assertProperty(json, "@version", 1);
      assertProperty(json, "logger_name", "com.example.Foo");

      assertTrue("Should contain @timestamp",  json.containsKey("@timestamp"));
      assertTrue("Should contain source_host", json.containsKey("source_host"));
      assertTrue("Should start with {", line.startsWith("{"));
      assertTrue("Should end with }", line.endsWith("}"));
    }

    Assert.assertEquals("Number of log events in the file", 6, lines.size());

    checkLine1 (lines.get(0));
    checkLine2 (lines.get(1));
    checkLine3 (lines.get(2));
    checkLine4 (lines.get(3));
    checkLine5 (lines.get(4));
    checkLine6 (lines.get(5));

  }

  private void checkLine1(String line) {
    JSONObject json = (JSONObject) JSONValue.parse(line);
    assertProperty(json, "level", "INFO");
    if (locationInfo) {
      assertProperty(json, "line_number", 13);
    } else {
      assertProperty(json, "line_number", null);
    }
    assertProperty(json, "message", "This is info");
    JSONObject mdc = (JSONObject) json.get("mdc");
    assertEquals("MDC should be empty", "{}", mdc.toString());
    assertTrue("MDC should be empty", mdc.isEmpty());
    JSONArray ndc = (JSONArray) json.get("ndc");
    assertEquals("NDC should be empty", "[]", ndc.toString());
    assertTrue("NDC should be empty", ndc.isEmpty());
  }

  private void checkLine2(String line) {
    JSONObject json = (JSONObject) JSONValue.parse(line);
    assertProperty(json, "level", "DEBUG");
    if (locationInfo) {
      assertProperty(json, "line_number", 15);
    } else {
      assertProperty(json, "message", "Some debug message");
    }
    JSONObject mdc = (JSONObject) json.get("mdc");
    assertTrue("MDC should be empty", mdc.isEmpty());
    JSONArray ndc = (JSONArray) json.get("ndc");
    assertEquals("NDC", "[\"my-ndc\"]", ndc.toString());
    assertTrue("NDC", ndc.contains("my-ndc"));
  }

  private void checkLine3(String line) {
    JSONObject json = (JSONObject) JSONValue.parse(line);
    assertProperty(json, "level", "WARN");
    assertProperty(json, "message", "A warning about the bad guys !");
    JSONObject mdc = (JSONObject) json.get("mdc");
    assertEquals("MDC should be Foo => Bar", "Bar", mdc.get("Foo"));
  }

  private void checkLine4(String line) {
    JSONObject json = (JSONObject) JSONValue.parse(line);
    assertProperty(json, "level", "ERROR");
    assertProperty(json, "message", "Too late: they are here ...");
    JSONObject mdc = (JSONObject) json.get("mdc");
    assertEquals("MDC should be Foo => Bar", "Bar", mdc.get("Foo"));
  }

  private void checkLine5(String line) {
    JSONObject json = (JSONObject) JSONValue.parse(line);
    assertProperty(json, "level", "FATAL");
    assertProperty(json, "message", "WE ARE DEAD");
    JSONObject mdc = (JSONObject) json.get("mdc");
    assertEquals("MDC should be empty", "{}", mdc.toString());
    JSONArray ndc = (JSONArray) json.get("ndc");
    assertEquals("NDC should be empty", "[]", ndc.toString());
    assertTrue("NDC should be empty", ndc.isEmpty());
  }

  private void checkLine6(String line) {
    JSONObject json = (JSONObject) JSONValue.parse(line);
    assertProperty(json, "level", "INFO");
    assertProperty(json, "message", LoggerHelper.ANOTHER_ONE_WITH_QUOTES_AND_DOUBLE_QUOTES_AND_AND_NEWLINES);
    JSONObject mdc = (JSONObject) json.get("mdc");
    assertEquals("MDC should be empty", "{}", mdc.toString());
    JSONArray ndc = (JSONArray) json.get("ndc");
    assertEquals("foo: {bar=baz, user=mickey}", ndc.get(0));
  }

  private void assertProperty(JSONObject json, String key, Object expected) {
    assertEquals(key, expected, json.get(key));
  }

}
