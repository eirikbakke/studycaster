package no.ebakke.studycaster.backend;

public class ConfigurationProperty {
  private Long   id;
  private String key;
  private String value;

  public ConfigurationProperty() {
  }

  public ConfigurationProperty(String key, String value) {
    this.key   = key;
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "ConfigurationProperty(" + id + ", " + key + ", " + value + ")";
  }
}
