package net.logstash.log4j;

import junit.framework.Assert;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;
import org.apache.log4j.MDC;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class JSONEventLayoutV0Test {
    static Logger logger;

    static JSONEventLayoutV0 layout = new JSONEventLayoutV0();

    static MockAppenderV0 appender;

    static final String[] logstashFields = new String[]{
            "@message",
            "@source_host",
            "@fields",
            "@timestamp"
    };

    @BeforeClass
    public static void setupTestAppender() {
        appender = new MockAppenderV0(layout);
        logger = Logger.getRootLogger();
        appender.setThreshold(Level.TRACE);
        appender.setName("mockappender");
        appender.activateOptions();
        logger.addAppender(appender);
    }

    @Before
    public void setupLayout() {
        layout.setAddRootThrowable(true);
        layout.setUserfields("application:ase,instance:001");
    }

    @After
    public void clearTestAppender() {
        NDC.clear();
        appender.clear();
        appender.close();
    }

    @Test
    public void testJSONEventLayoutIsJSON() {
        logger.info("this is an info message");
        String message = appender.getMessages()[0];
        Assert.assertTrue("Event is not valid JSON", JSONValue.isValidJsonStrict(message));
    }

    @Test
    public void testJSONEventLayoutHasKeys() {
        logger.info("this is a test message");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;

        for (String fieldName : logstashFields) {
            Assert.assertTrue("Event does not contain field: " + fieldName, jsonObject.containsKey(fieldName));
        }
    }

    @Test
    public void testJSONEventLayoutHasFieldLevel() {
        logger.fatal("this is a new test message");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");

        Assert.assertEquals("Log level is wrong", "FATAL", atFields.get("level"));
    }

    @Test
    public void testJSONEventLayoutHasNDC() {
        String ndcData = new String("json-layout-test");
        NDC.push(ndcData);
        logger.warn("I should have NDC data in my log");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");

        Assert.assertEquals("NDC is wrong", ndcData, atFields.get("ndc"));
    }

    @Test
    public void testJSONEventLayoutHasMDC() {
        MDC.put("foo","bar");
        logger.warn("I should have MDC data in my log");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");
        JSONObject mdcData = (JSONObject) atFields.get("mdc");

        Assert.assertEquals("MDC is wrong","bar", mdcData.get("foo"));
    }

    @Test
    public void testJSONEventLayoutExceptions() {
        String exceptionMessage = new String("shits on fire, yo");
        logger.fatal("uh-oh", new IllegalArgumentException(exceptionMessage));
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");
        JSONObject exceptionInformation = (JSONObject) atFields.get("exception");

        Assert.assertEquals("Exception class missing", "java.lang.IllegalArgumentException", exceptionInformation.get("exception_class"));
        Assert.assertEquals("Root exception class missing", "java.lang.IllegalArgumentException", exceptionInformation.get("root_exception_class"));
        Assert.assertEquals("Exception exception message", exceptionMessage, exceptionInformation.get("exception_message"));
        Assert.assertEquals("Root exception exception message", exceptionMessage, exceptionInformation.get("root_exception_message"));
        Assert.assertNotNull("Exception stacktrace is null", exceptionInformation.get("stacktrace"));
        Assert.assertNotNull("Exception root stacktrace is null", exceptionInformation.get("root_stacktrace"));
        Assert.assertTrue("Exception stacktrace isn't equal to root exception stacktrace", exceptionInformation.get("stacktrace").equals(exceptionInformation.get("root_stacktrace")));
        Assert.assertTrue("Exception stacktrace doesn't contain a reference to the exception", exceptionInformation.get("stacktrace").toString().contains("java.lang.IllegalArgumentException"));
    }

    @Test
    public void testJSONEventLayoutRootExceptions() {
        String exceptionMessage = new String("down below the shits on fire, yo");
        String rootExceptionMessage = new String("shits on fire, yo");
        Exception root = new IllegalStateException(rootExceptionMessage);

        logger.fatal("uh-oh", new IllegalArgumentException(exceptionMessage, root));

        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");
        JSONObject exceptionInformation = (JSONObject) atFields.get("exception");

        Assert.assertEquals("Exception class missing", "java.lang.IllegalArgumentException", exceptionInformation.get("exception_class"));
        Assert.assertEquals("Root exception class missing", "java.lang.IllegalStateException", exceptionInformation.get("root_exception_class"));
        Assert.assertEquals("Exception exception message", exceptionMessage, exceptionInformation.get("exception_message"));
        Assert.assertEquals("Root exception exception message", rootExceptionMessage, exceptionInformation.get("root_exception_message"));
        Assert.assertNotNull("Exception stacktrace is null", exceptionInformation.get("stacktrace"));
        Assert.assertNotNull("Exception root stacktrace is null", exceptionInformation.get("root_stacktrace"));
        Assert.assertFalse("Exception stacktrace is the same as the root exception stacktrace", exceptionInformation.get("stacktrace").equals(exceptionInformation.get("root_stacktrace")));
        Assert.assertTrue("Exception stacktrace doesn't contain a reference to the exception", exceptionInformation.get("stacktrace").toString().contains("java.lang.IllegalArgumentException"));
        Assert.assertTrue("Exception stacktrace doesn't contain a reference to the root exception", exceptionInformation.get("stacktrace").toString().contains("java.lang.IllegalStateException"));
        Assert.assertFalse("Exception stacktrace contains a reference to the top exception", exceptionInformation.get("root_stacktrace").toString().contains("java.lang.IllegalArgumentException"));
        Assert.assertTrue("Exception stacktrace doesn't contain a reference to the root exception", exceptionInformation.get("root_stacktrace").toString().contains("java.lang.IllegalStateException"));
    }

    @Test
    public void testJSONEventLayoutNoRootExceptions() {
        layout.setAddRootThrowable(false);

        String exceptionMessage = new String("down below the shits on fire, yo");
        String rootExceptionMessage = new String("shits on fire, yo");
        Exception root = new IllegalStateException(rootExceptionMessage);

        logger.fatal("uh-oh", new IllegalArgumentException(exceptionMessage, root));

        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");
        JSONObject exceptionInformation = (JSONObject) atFields.get("exception");

        Object a = exceptionInformation.get("root_exception_class");

        Assert.assertNull("Exception root class not null", exceptionInformation.get("root_exception_class"));
        Assert.assertNull("Exception root message not null", exceptionInformation.get("root_exception_message"));
        Assert.assertNull("Exception root stacktrace not null", exceptionInformation.get("root_stacktrace"));
    }

    @Test
    public void testJSONEventLayoutHasClassName() {
        logger.warn("warning dawg");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");

        Assert.assertEquals("Logged class does not match", this.getClass().getCanonicalName().toString(), atFields.get("class"));
    }

    @Test
    public void testJSONEventHasFileName() {
        logger.warn("whoami");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");

        Assert.assertNotNull("File value is missing", atFields.get("file"));
    }

    @Test
    public void testJSONEventHasLoggerName() {
        logger.warn("whoami");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");
        Assert.assertNotNull("LoggerName value is missing", atFields.get("loggerName"));
    }

    @Test
    public void testJSONEventHasThreadName() {
        logger.warn("whoami");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");
        Assert.assertNotNull("ThreadName value is missing", atFields.get("threadName"));
    }

    @Test
    public void testJSONEventLayoutNoLocationInfo() {
        JSONEventLayoutV0 layout = (JSONEventLayoutV0) appender.getLayout();
        boolean prevLocationInfo = layout.getLocationInfo();

        layout.setLocationInfo(false);

        logger.warn("warning dawg");
        String message = appender.getMessages()[0];
        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");

        Assert.assertFalse("atFields contains file value", atFields.containsKey("file"));
        Assert.assertFalse("atFields contains line_number value", atFields.containsKey("line_number"));
        Assert.assertFalse("atFields contains class value", atFields.containsKey("class"));
        Assert.assertFalse("atFields contains method value", atFields.containsKey("method"));

        // Revert the change to the layout to leave it as we found it.
        layout.setLocationInfo(prevLocationInfo);
    }

    @Test
    @Ignore
    public void measureJSONEventLayoutLocationInfoPerformance() {
        JSONEventLayoutV0 layout = (JSONEventLayoutV0) appender.getLayout();
        boolean locationInfo = layout.getLocationInfo();
        int iterations = 100000;
        long start, stop;

        start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            logger.warn("warning dawg");
        }
        stop = System.currentTimeMillis();
        long firstMeasurement = stop - start;

        layout.setLocationInfo(!locationInfo);
        start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            logger.warn("warning dawg");
        }
        stop = System.currentTimeMillis();
        long secondMeasurement = stop - start;

        System.out.println("First Measurement (locationInfo: " + locationInfo + "): " + firstMeasurement);
        System.out.println("Second Measurement (locationInfo: " + !locationInfo + "): " + secondMeasurement);

        // Clean up
        layout.setLocationInfo(!locationInfo);
    }

    @Test
    public void testDateFormat() {
        long timestamp = 1364844991207L;
        Assert.assertEquals("format does not produce expected output", "2013-04-01T19:36:31.207Z", JSONEventLayoutV0.dateFormat(timestamp));
    }
    
    @Test
    public void testUserFields() {
        logger.info("this is an info message");
        String message = appender.getMessages()[0];

        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");
        Assert.assertTrue("atFields should contain application value", atFields.containsKey("application"));
        Assert.assertTrue("atFields should contain instance value", atFields.containsKey("instance"));                        
    }

    @Test
    public void testJsonParser() {
        logger.info("{'message': 'test', 'deeper': [1, 2, 3], 'nested': {'some': 'thing'}}");
        String message = appender.getMessages()[0];

        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertEquals("test", jsonObject.get("@message"));

        JSONObject atFields = (JSONObject) jsonObject.get("@fields");
        Assert.assertTrue(atFields.containsKey("context"));
        JSONObject context = (JSONObject) atFields.get("context");

        Assert.assertEquals(2, context.size());
        JSONObject nested = (JSONObject) context.get("nested");
        JSONArray deeper = (JSONArray) context.get("deeper");

        Assert.assertNotNull(nested);
        Assert.assertNotNull(deeper);

        Assert.assertEquals("[1,2,3]", deeper.toString());
        Assert.assertEquals("thing", nested.get("some"));
    }
    

    @Test
    public void testInvalidJson() {
        String invalidJson = "{not_json: [in brackets}";
        logger.info(invalidJson);
        String message = appender.getMessages()[0];

        Object obj = JSONValue.parse(message);
        JSONObject jsonObject = (JSONObject) obj;
        Assert.assertEquals(invalidJson, jsonObject.get("@message"));
        JSONObject atFields = (JSONObject) jsonObject.get("@fields");
        Assert.assertFalse(atFields.containsKey("context"));
    }
}
