package net.logstash.log4j2;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.message.Message;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Message implementation that combines a String message with a map of fields.
 *
 *
 */
public class MessageMapMessage implements Message{
  private static final Pattern fieldPattern = Pattern.compile("\\{(.+?)\\}");

  private String message;

  private Map<String, Object> fields = new TreeMap<>();
  private Map<String, String> fieldsAsString = null;

  public MessageMapMessage(String message){
    this.message = message;
  }

  public MessageMapMessage add(String field, Object value){
    this.fields.put(field, value);
    fieldsAsString = null;
    return this;
  }

  @Override
  public String getFormattedMessage() {
    fieldsAsString = new HashMap<>();

    //TODO for now just append the fields to the message
    if (StringUtils.isBlank(message)){
      return message;
    }

    StringBuffer sb = new StringBuffer(message.length());
    Matcher m = fieldPattern.matcher(message);
    while (m.find()){
      String found = m.group(1);

      String replacement = null;
      if (fields.containsKey(found)){
        replacement = Objects.toString(fields.get(found));
        fieldsAsString.put(found, replacement);
      } else {
        replacement = "{"+found+"}";
      }

      m.appendReplacement(sb, replacement);
    }
    m.appendTail(sb);

    return sb.toString();
  }

  public Map<String, String> getFieldsAsString(){
    if (fieldsAsString == null){
      fieldsAsString = new HashMap<>();
    }

    // add all fields for which we don't have a string yet
    for (String key : fields.keySet()){
      if (!fieldsAsString.containsKey(key)){
        fieldsAsString.put(key, Objects.toString(fields.get(key)));
      }
    }

    return fieldsAsString;
  }


  @Override
  public String getFormat() {
    return message;
  }

  @Override
  public Object[] getParameters() {
    return fields.values().toArray();
  }

  @Override
  public Throwable getThrowable() {
    return null;// TODO
  }

  public Map<String, Object> getFields() {
    return fields;
  }
}
