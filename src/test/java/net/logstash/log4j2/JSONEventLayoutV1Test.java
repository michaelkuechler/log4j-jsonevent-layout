package net.logstash.log4j2;

import junit.framework.Assert;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.junit.*;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@SuppressWarnings("AccessStaticViaInstance")
public class JSONEventLayoutV1Test {

  static final String userFieldsSingle = "field1:value1";
  static final String userFieldsMulti = "field2:value2,field3:value3";
  static final String userFieldsSingleProperty = "field1:propval1";

  static final String[] logstashFields = new String[]{
      "message",
      "source_host",
      "@timestamp",
      "@version"
  };

  @AfterClass
  public static void cleanUp() {
    System.clearProperty(JSONEventLayoutV1.USER_FIELDS_PROPERTY);
  }

  private JSONObject formatLogEvent() {
    return formatLogEvent(true, userFieldsSingle);
  }

  private JSONObject formatLogEvent(boolean locationInfo, String userFields) {
    JSONEventLayoutV1 layout = JSONEventLayoutV1.createLayout(locationInfo, userFields, Charset.forName("UTF-8"));
    final Log4jLogEvent event = LogEventFixtures.createLogEvent();
    String output = layout.toSerializable(event);
    Object obj = JSONValue.parse(output);
    Assert.assertTrue("Output should be valid JSON", JSONValue.isValidJsonStrict(output));
    return (JSONObject) obj;
  }

  @Test
  public void testJSONEventLayoutIsJSON() {
    final Log4jLogEvent event = LogEventFixtures.createLogEvent();
    JSONEventLayoutV1 layout = JSONEventLayoutV1.createLayout(true, null, Charset.forName("UTF-8"));
    String output = layout.toSerializable(event);
    Assert.assertTrue("Output should be valid JSON", JSONValue.isValidJsonStrict(output));
  }

  @Test
  public void testJSONEventLayoutHasUserFieldsFromProps() {
    System.setProperty(JSONEventLayoutV1.USER_FIELDS_PROPERTY, userFieldsSingleProperty);
    JSONObject jsonObject = formatLogEvent();
    Assert.assertTrue("Event does not contain field 'field1'" , jsonObject.containsKey("field1"));
    Assert.assertEquals("Event does not contain value 'value1'", "propval1", jsonObject.get("field1"));
    System.clearProperty(JSONEventLayoutV1.USER_FIELDS_PROPERTY);
  }

  @Test
  public void testJSONEventLayoutHasUserFieldsFromConfig() {
    final Log4jLogEvent event = LogEventFixtures.createLogEvent();
    JSONEventLayoutV1 layout = JSONEventLayoutV1.createLayout(true, userFieldsSingle, Charset.forName("UTF-8"));
    String output = layout.toSerializable(event);
    Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(output));
    Object obj = JSONValue.parse(output);
    JSONObject jsonObject = (JSONObject) obj;
    Assert.assertTrue("Event does not contain field 'field1'" , jsonObject.containsKey("field1"));
    Assert.assertEquals("Event does not contain value 'value1'", "value1", jsonObject.get("field1"));
  }

  @Test
  public void testJSONEventLayoutUserFieldsMulti() {
    final Log4jLogEvent event = LogEventFixtures.createLogEvent();
    JSONEventLayoutV1 layout = JSONEventLayoutV1.createLayout(true, userFieldsMulti, Charset.forName("UTF-8"));
    String output = layout.toSerializable(event);
    Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(output));
    Object obj = JSONValue.parse(output);
    JSONObject jsonObject = (JSONObject) obj;
    Assert.assertTrue("Event does not contain field 'field2'" , jsonObject.containsKey("field2"));
    Assert.assertEquals("Event does not contain value 'value2'", "value2", jsonObject.get("field2"));
    Assert.assertTrue("Event does not contain field 'field3'" , jsonObject.containsKey("field3"));
    Assert.assertEquals("Event does not contain value 'value3'", "value3", jsonObject.get("field3"));
  }

  @Test
  public void testJSONEventLayoutUserFieldsPropOverride() {
    // set the property first
    System.setProperty(JSONEventLayoutV1.USER_FIELDS_PROPERTY, userFieldsSingleProperty);
    JSONEventLayoutV1 layout = JSONEventLayoutV1.createLayout(true, userFieldsSingle, Charset.forName("UTF-8"));
    final Log4jLogEvent event = LogEventFixtures.createLogEvent();
    String output = layout.toSerializable(event);
    Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(output));
    Object obj = JSONValue.parse(output);
    JSONObject jsonObject = (JSONObject) obj;
    Assert.assertTrue("Event does not contain field 'field1'" , jsonObject.containsKey("field1"));
    Assert.assertEquals("Event does not contain value 'propval1'", "propval1", jsonObject.get("field1"));
    System.clearProperty(JSONEventLayoutV1.USER_FIELDS_PROPERTY);
  }

  @Test
  public void testJSONEventLayoutHasLogStashFields() {
    Object obj = formatLogEvent();
    JSONObject jsonObject = (JSONObject) obj;
    for (String fieldName : logstashFields) {
      Assert.assertTrue("Event does not contain field: " + fieldName, jsonObject.containsKey(fieldName));
    }
  }

  @Test
  public void testJSONEventLayoutHasNDC() {
    JSONObject jsonObject = formatLogEvent();
    Assert.assertEquals("ndc should be an array", JSONArray.class, jsonObject.get("ndc").getClass());
    JSONArray ndc = (JSONArray) jsonObject.get("ndc");
    Assert.assertEquals("NDC is wrong", "stack_msg1", ndc.get(0));
    Assert.assertEquals("NDC is wrong", "stack_msg2", ndc.get(1));
    Assert.assertEquals("NDC is wrong", "[\"stack_msg1\",\"stack_msg2\"]", jsonObject.get("ndc").toString());
  }

  @Test
  public void testJSONEventLayoutHasMDC() {
    Object obj = formatLogEvent();
    JSONObject jsonObject = (JSONObject) obj;
    JSONObject mdc = (JSONObject) jsonObject.get("mdc");
    Assert.assertEquals("MDC is wrong","A_Value", mdc.get("MDC.A"));
    Assert.assertEquals("MDC is wrong","B_Value", mdc.get("MDC.B"));
  }

  @Test
  public void testJSONEventLayoutExceptions() {
    JSONEventLayoutV1 layout = JSONEventLayoutV1.createLayout(true, userFieldsSingle, Charset.forName("UTF-8"));
    final Log4jLogEvent event = LogEventFixtures.createLogEvent();
    String output = layout.toSerializable(event);
    Object obj = JSONValue.parse(output);
    JSONObject jsonObject = (JSONObject) obj;
    JSONObject exceptionInformation = (JSONObject) jsonObject.get("exception");
    Assert.assertEquals("Exception class missing", "java.io.IOException", exceptionInformation.get("exception_class"));
    Assert.assertEquals("Exception exception message", "testIOEx", exceptionInformation.get("exception_message"));
  }

  @Test
  public void testGetContentType() {
    JSONEventLayoutV1 layout = JSONEventLayoutV1.createLayout(true, userFieldsSingle, Charset.forName("UTF-8"));
    assertEquals("application/json; charset=UTF-8",layout.getContentType());
  }


  @Test
  public void testJSONEventLayoutHasClassName() {
    JSONObject jsonObject = formatLogEvent();
    Assert.assertEquals("Logged class does not match", "net.logstash.log4j2.LogEventFixtures", jsonObject.get("class"));
  }

  @Test
  public void testJSONEventHasFileName() {
    JSONObject jsonObject = formatLogEvent();
    Assert.assertNotNull("File value is missing", jsonObject.get("file"));
  }

  @Test
  public void testJSONEventHasLoggerName() {
    JSONObject jsonObject = formatLogEvent();
    Assert.assertNotNull("LoggerName value is missing", jsonObject.get("logger_name"));
  }

  @Test
  public void testJSONEventHasThreadName() {
    JSONObject jsonObject = formatLogEvent();
    Assert.assertNotNull("ThreadName value is missing", jsonObject.get("thread_name"));
  }

  @Test
  public void testKeys() {
    JSONObject jsonObject = formatLogEvent(true, userFieldsMulti);
    Set<String> expected = new HashSet<>();
    expected.add("exception");
    expected.add("source_host");
    expected.add("method");
    expected.add("level");
    expected.add("message");
    expected.add("ndc");
    expected.add("mdc");
    expected.add("@timestamp");
    expected.add("file");
    expected.add("line_number");
    expected.add("thread_name");
    expected.add("@version");
    expected.add("logger_name");
    expected.add("field3");
    expected.add("field2");
    expected.add("class");
    Assert.assertEquals(expected, jsonObject.keySet());
    for (String key : expected) {
      Assert.assertTrue("JSON should contain key [" + key + "]",  jsonObject.keySet().contains(key));
    }
  }

  @Test
  public void testFields() {

    // "timeMillis"   => @timestamp
    // "thread"       => thread_name
    // "level"        => level
    // "loggerName"   => logger_name
    // "marker"       => TODO
    // "name"         => TODO ??
    // "parents"      => TODO ??
    // "message"      => message
    // "thrown"       => TODO ?
    // "cause"        => TODO ?
    // "class"        => class
    // "method"       => method
    // "file"         => file
    // "line"         => line_number
    // "exact"        => TODO ?
    // "location"     => TODO ?
    // "version"      => @Version ?
    // "commonElementCount"  =>
    // "localizedMessage"  =>
    // "extendedStackTrace"  =>
    // "suppressed"  =>
    // "loggerFqcn"  =>
    // "endOfBatch"  =>
  }



  @Test
  public void testJSONEventLayoutWithLocationInfo() {
    JSONObject jsonObject = formatLogEvent(true, userFieldsMulti);
    System.out.println("jsonObject = " + jsonObject.keySet());
    Assert.assertTrue("event contains file value", jsonObject.containsKey("file"));
    Assert.assertTrue("event contains line_number value", jsonObject.containsKey("line_number"));
    Assert.assertTrue("event contains class value", jsonObject.containsKey("class"));
    Assert.assertTrue("event contains method value", jsonObject.containsKey("method"));

  }


  @Test
  public void testJSONEventLayoutWithoutLocationInfo() {
    JSONObject jsonObject = formatLogEvent(false, userFieldsMulti);
    Assert.assertFalse("atFields contains file value", jsonObject.containsKey("file"));
    Assert.assertFalse("atFields contains line_number value", jsonObject.containsKey("line_number"));
    Assert.assertFalse("atFields contains class value", jsonObject.containsKey("class"));
    Assert.assertFalse("atFields contains method value", jsonObject.containsKey("method"));
  }

  @Test
  public void testLevelInJSON() {
    JSONObject jsonObject = formatLogEvent();
    Assert.assertEquals("DEBUG", jsonObject.get("level"));
  }

  @Test
  public void testLevelWarning() {
    JSONEventLayoutV1 layout = JSONEventLayoutV1.createLayout(true, userFieldsSingle, Charset.forName("UTF-8"));
    final Log4jLogEvent event = LogEventFixtures.createSimpleLogEvent(true);
    String output = layout.toSerializable(event);
    JSONObject jsonObject = (JSONObject) JSONValue.parse(output);
    for (String fieldName : logstashFields) {
      Assert.assertTrue("Event does not contain field: " + fieldName, jsonObject.containsKey(fieldName));
    }
    Assert.assertEquals("WARN", jsonObject.get("level"));
  }

  @Test
  public void testTimestamp() {
    JSONEventLayoutV1 layout = JSONEventLayoutV1.createLayout(true, userFieldsSingle, Charset.forName("UTF-8"));
    final Log4jLogEvent event = LogEventFixtures.createSimpleLogEvent(true);
    String output = layout.toSerializable(event);
    JSONObject jsonObject = (JSONObject) JSONValue.parse(output);
    Assert.assertEquals("2013-04-01T19:36:31.207Z", jsonObject.get("@timestamp"));
  }

  @Test
  public void testDateFormat() {
    long timestamp = 1364844991207L;
    String expected = "2013-04-01T19:36:31.207Z";
    String actual = JSONEventLayoutV1.dateFormat(timestamp);
    Assert.assertEquals("format does not produce expected output", expected, actual);
  }
}
