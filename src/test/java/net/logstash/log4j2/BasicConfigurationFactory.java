package net.logstash.log4j2;

import java.net.URI;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;

/**
 *
 */
public class BasicConfigurationFactory extends ConfigurationFactory {

  @Override
  public Configuration getConfiguration(final String name, final URI configLocation) {
    return new BasicConfiguration();
  }

  @Override
  public String[] getSupportedTypes() {
    return null;
  }

  @Override
  public Configuration getConfiguration(final ConfigurationSource source) {
    return null;
  }

  public class BasicConfiguration extends AbstractConfiguration {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_LEVEL = "org.apache.logging.log4j.level";

    public BasicConfiguration() {
      super(ConfigurationSource.NULL_SOURCE);

      final LoggerConfig root = getRootLogger();
      final String name = System.getProperty(DEFAULT_LEVEL);
      final Level level = (name != null && Level.getLevel(name) != null) ? Level.getLevel(name) : Level.ERROR;
      root.setLevel(level);
    }
  }
}
