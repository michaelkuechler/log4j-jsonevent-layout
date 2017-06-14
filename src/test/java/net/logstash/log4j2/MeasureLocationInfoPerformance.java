package net.logstash.log4j2;

import org.apache.logging.log4j.core.layout.JsonLayout;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.Charset;

public class MeasureLocationInfoPerformance {

  @Ignore
  @Test
  public void measureJSONEventLayoutLocationInfoPerformance() {
    int iterations = 5000;

    // warm-up

    formatManyWithStandardJsonLayout(true, iterations);
    formatManyWithStandardJsonLayout(false, iterations);
    formatMany(true,  iterations);
    formatMany(false, iterations);

    // measure
    iterations = 100000;

    long third = formatManyWithStandardJsonLayout(true, iterations);
    long fourth = formatManyWithStandardJsonLayout(false, iterations);

    long firstMeasurement  = formatMany(true,  iterations);
    long secondMeasurement = formatMany(false, iterations);




    float percentage = 100.0f* (firstMeasurement - secondMeasurement)/secondMeasurement;
    System.out.println("With locationInfo is " + percentage + "% slower than without.");
    percentage = 100.0f* (secondMeasurement - fourth)/fourth;
    System.out.println("JSONEventLayoutV1 is " + percentage + "% slower than JsonLayout.");

  }

  private long formatMany(boolean locationInfo, int iterations) {
    JSONEventLayoutV1 layout = JSONEventLayoutV1.createLayout(locationInfo, null, Charset.forName("UTF-8"));
    long totalLength = 0;
    long start, stop;
    start = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      String json = layout.toSerializable(LogEventFixtures.createSimpleLogEvent(locationInfo));
      totalLength = totalLength + json.length();
//      System.out.println("JSONEventLayoutV1: " + locationInfo + " => length = " + json.length());
//      System.out.println("json = \n" + json);
    }
    stop = System.currentTimeMillis();
    long millis = stop - start;
    float perEvent = 1.0f * millis/iterations;
    String info = String.format("%s locationInfo:[%s] millis=[%d] avg=%f length=[%d] ",
        layout.getClass().getSimpleName(), locationInfo, millis, perEvent, totalLength);
    System.out.println(info);

    return millis;
  }

  private long formatManyWithStandardJsonLayout(boolean locationInfo, int iterations) {
    JsonLayout layout = (JsonLayout) JsonLayout.createLayout(locationInfo,true,false,true,false,Charset.forName("UTF-8"));
    long totalLength = 0;

    long start, stop;
    start = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      String json = layout.toSerializable(LogEventFixtures.createSimpleLogEvent(locationInfo));
      totalLength = totalLength + json.length();
    }
    stop = System.currentTimeMillis();
    long millis = stop - start;
    float perEvent = 1.0f * millis/iterations;
    String info = String.format("%s locationInfo:[%s] millis=[%d] avg=%f length=[%d] ",
        layout.getClass().getSimpleName(), locationInfo, millis, perEvent, totalLength);
    System.out.println(info);
    return millis;

  }

}
