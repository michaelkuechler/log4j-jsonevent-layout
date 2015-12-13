package net.logstash.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.util.HashMap;

public class LoggerHelper {

  public void logSomeEvents() {
    Logger logger = LogManager.getLogger("com.example.Foo");
    logger.info("This is info");
    ThreadContext.push("my-ndc");
    logger.debug("Some debug message");
    ThreadContext.put("Foo", "Bar");
    logger.warn("A warning about the bad guys !");
    logger.error("Too late: they are here ...");
    ThreadContext.clearAll();
    logger.fatal("WE ARE DEAD");

    HashMap<String, String> nestedMdc = new HashMap<>();
    nestedMdc.put("bar","baz");
    nestedMdc.put("user","mickey");
    ThreadContext.push("foo: {}", nestedMdc);

    logger.info(ANOTHER_ONE_WITH_QUOTES_AND_DOUBLE_QUOTES_AND_AND_NEWLINES);
    ThreadContext.clearAll();
  }

  public static final String ANOTHER_ONE_WITH_QUOTES_AND_DOUBLE_QUOTES_AND_AND_NEWLINES
      = "Another {bar} one with 'quotes' and \"double-quotes\" and {} and \n newlines ";

}
