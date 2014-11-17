package net.logstash.log4j;

import net.logstash.log4j.data.HostData;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class JSONEventLayout extends Layout {

    private String userfields; // comma separated, colon separated key value pairs    
    private boolean locationInfo = false;

    private boolean ignoreThrowable = false;

    private String hostname = new HostData().getHostName();
    private String threadName;
    private long timestamp;
    private String ndc;
    private Map<?,?> mdc;
    private LocationInfo info;
    private HashMap<String, Object> fieldData;
    private HashMap<String, Object> exceptionInformation;

    private JSONObject logstashEvent;

    public static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    public static final FastDateFormat ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", UTC);

    public static String dateFormat(long timestamp) {
        return ISO_DATETIME_TIME_ZONE_FORMAT_WITH_MILLIS.format(timestamp);
    }

    /**
     * For backwards compatibility, the default is to generate location information
     * in the log messages.
     */
    public JSONEventLayout() {
        this(true);
    }

    /**
     * Creates a layout that optionally inserts location information into log messages.
     *
     * @param locationInfo whether or not to include location information in the log messages.
     */
    public JSONEventLayout(boolean locationInfo) {
        this.locationInfo = locationInfo;
    }

    public String format(LoggingEvent loggingEvent) {
        threadName = loggingEvent.getThreadName();
        timestamp = loggingEvent.getTimeStamp();
        fieldData = new HashMap<String, Object>();
        exceptionInformation = new HashMap<String, Object>();
        mdc = loggingEvent.getProperties();
        ndc = loggingEvent.getNDC();

        logstashEvent = new JSONObject();

        logstashEvent.put("@source_host", hostname);
        appendMessage(loggingEvent);
        logstashEvent.put("@timestamp", dateFormat(timestamp));

        if (loggingEvent.getThrowableInformation() != null) {
            final ThrowableInformation throwableInformation = loggingEvent.getThrowableInformation();
            if (throwableInformation.getThrowable().getClass().getCanonicalName() != null) {
                exceptionInformation.put("exception_class", throwableInformation.getThrowable().getClass().getCanonicalName());
            }
            if (throwableInformation.getThrowable().getMessage() != null) {
                exceptionInformation.put("exception_message", throwableInformation.getThrowable().getMessage());
            }
            if (throwableInformation.getThrowableStrRep() != null) {
                String stackTrace = StringUtils.join(throwableInformation.getThrowableStrRep(), "\n");
                exceptionInformation.put("stacktrace", stackTrace);
            }
            addFieldData("exception", exceptionInformation);
        }

        if (locationInfo) {
            info = loggingEvent.getLocationInformation();
            addFieldData("file", info.getFileName());
            addFieldData("line_number", info.getLineNumber());
            addFieldData("class", info.getClassName());
            addFieldData("method", info.getMethodName());
        }

        addFieldData("loggerName", loggingEvent.getLoggerName());
        addFieldData("mdc", mdc);
        addFieldData("ndc", ndc);
        addFieldData("level", loggingEvent.getLevel().toString());
        addFieldData("threadName", threadName);

        // Add userfields if provided
        if (this.userfields != null) {
            for (String pairs : this.userfields.split(",")) {
                String[] pair = pairs.split(":");
                if (pair.length == 2) {
                    this.addFieldData(pair[0], pair[1]);
                }
            }
        }                
        logstashEvent.put("@fields", fieldData);        
        return logstashEvent.toString() + "\n";
    }

    private void appendMessage(LoggingEvent loggingEvent) {
        String message = loggingEvent.getRenderedMessage();
        if (isJson(message)) {
            parseJson(message);
        } else {
            logstashEvent.put("@message", message);
        }
    }

    private boolean isJson(String message) {
        return message.startsWith("{") && message.endsWith("}");
    }

    private void parseJson(String message) {
        JSONObject json = (JSONObject) JSONValue.parse(message);
        if (json != null) {
            if (json.containsKey("message")) {
                logstashEvent.put("@message", json.get("message"));
                json.remove("message");
                fieldData.put("context", json);
            } else {
                logstashEvent.put("@message", message);
            }
        }
    }

    public boolean ignoresThrowable() {
        return ignoreThrowable;
    }

    /**
     * Query whether log messages include location information.
     *
     * @return true if location information is included in log messages, false otherwise.
     */
    public boolean getLocationInfo() {
        return locationInfo;
    }

    /**
     * Set whether log messages should include location information.
     *
     * @param locationInfo true if location information should be included, false otherwise.
     */
    public void setLocationInfo(boolean locationInfo) {
        this.locationInfo = locationInfo;
    }

    public void activateOptions() {
        //Ignored
    }

    private void addFieldData(String keyname, Object keyval) {
        if (null != keyval) {
            fieldData.put(keyname, keyval);
        }
    }
    
    /**
     * Add fieldData from the provided userfields.   
     * @param userfields comma separated, colon separated key value pairs
     */
    public void setUserfields(String userfields) {
        this.userfields = userfields;
    }
}
