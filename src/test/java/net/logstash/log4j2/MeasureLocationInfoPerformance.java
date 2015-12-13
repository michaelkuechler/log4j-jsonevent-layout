package net.logstash.log4j2;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.Charset;

public class MeasureLocationInfoPerformance {

  @Ignore
  @Test
  public void measureJSONEventLayoutLocationInfoPerformance() {
    int iterations = 100000;
    long firstMeasurement  = formatMany(true,  iterations);
    long secondMeasurement = formatMany(false, iterations);

    System.out.println("First  Measurement (locationInfo: true)  : " + firstMeasurement);
    System.out.println("Second Measurement (locationInfo: false) : " + secondMeasurement);

    float percentage = 100.0f* (firstMeasurement - secondMeasurement)/secondMeasurement;
    float perEventWith    = 1.0f * firstMeasurement/iterations;
    float perEventWithout = 1.0f * secondMeasurement/iterations;

    System.out.println("Average per event (locationInfo: true)  : " + perEventWith    + " ms");
    System.out.println("Average per event (locationInfo: false) : " + perEventWithout + " ms");
    System.out.println("With locationInfo is " + percentage + "% slower than without.");
  }

  private long formatMany(boolean locationInfo, int iterations) {
    JSONEventLayoutV1 layout = JSONEventLayoutV1.createLayout(locationInfo, null, Charset.forName("UTF-8"));

    long start, stop;
    start = System.currentTimeMillis();
    for (int i = 0; i < iterations; i++) {
      layout.toSerializable(LogEventFixtures.createSimpleLogEvent(locationInfo));
    }
    stop = System.currentTimeMillis();
    return stop - start;
  }

}
