package SITE.RECIPICK.RECIPICK_PROJECT.dto;

public record WeatherData(Double temp, Double rain, Double snow) {
  public WeatherData {
    temp = norm(temp);
    rain = norm(rain);
    snow = norm(snow);
  }
  private static Double norm(Double v) {
    if (v == null) return null;
    return v <= -8.9 ? null : v; // -9.* => null
  }
}