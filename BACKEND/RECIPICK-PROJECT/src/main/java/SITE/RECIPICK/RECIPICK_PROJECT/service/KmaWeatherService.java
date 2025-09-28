package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.dto.WeatherData;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class KmaWeatherService {

  private final RestClient rest = RestClient.builder()
      .requestFactory(new SimpleClientHttpRequestFactory() {{
        setConnectTimeout(1500);
        setReadTimeout(1500);
      }})
      .defaultHeader("User-Agent", "Recipick/1.0 (+recipick)")
      .build();

  @Value("${kma.apiKey}")
  private String apiKey;

  // 임계값
  private static final double RAIN_MM = 0.1, SNOW_MM = 0.1, HOT_MIN = 22.0, COLD_MAX = 14.0;

  // 조회 범위(10분 단위, 최대 60분)
  private static final int STEP_MIN = 10, MAX_STEPS = 6;

  // ====== 앱 전역 캐시 ======
  private volatile WeatherData cachedData;
  private volatile String cachedCond = "normal";
  private volatile long cachedAtMs = 0L;

  // 앱 기동 직후 한 번 갱신
  @PostConstruct
  public void initWarmUp() {
    try {
      refreshCache(108);
    } catch (Exception e) {
      log.warn("[KMA] 초기 캐시 갱신 실패: {}", e.getMessage());
    }
  }

  // 매 분 1회 갱신 (원하면 */120 등으로 조절)
  @Scheduled(cron = "0 * * * * *")
  public void scheduledRefresh() {
    try {
      refreshCache(108);
    } catch (Exception e) {
      log.warn("[KMA] 스케줄 캐시 갱신 실패: {}", e.getMessage());
    }
  }

  /** 메인/컨트롤러는 이것만 호출: 항상 즉시 반환 */
  public String getCachedConditionOrNormal() {
    return cachedCond == null ? "normal" : cachedCond;
  }

  /** 마지막 캐시 데이터(디버그/로그용) */
  public WeatherData lastCachedWeather() {
    return cachedData;
  }

  // ===== 내부 구현 =====

  private void refreshCache(int stn) {
    String body = fetchLatestObs(stn);        // 네트워크는 여기서만, 스케줄 타이밍에!
    WeatherData w = (body == null) ? null : parse(body);
    String cond = conditionFrom(w);
    cachedData = w;
    cachedCond = cond;
    cachedAtMs = System.currentTimeMillis();
    log.debug("[KMA] 캐시 갱신 cond={}, data={}, at={}", cond, w, cachedAtMs);
  }

  /** KMA 원 호출 */
  private String getObsData(String tm, int stn) {
    String url = "https://apihub.kma.go.kr/api/typ01/url/kma_sfctm2.php"
        + "?tm=" + tm + "&stn=" + stn + "&help=0" + "&authKey=" + apiKey;
    return rest.get().uri(url).retrieve().body(String.class);
  }

  /** 가까운 과거(최대 60분)에서 첫 유효 데이터 라인 찾기 (짧은 타임아웃) */
  private String fetchLatestObs(int stn) {
    var now = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Seoul"))
        .withSecond(0).withNano(0);
    int m10 = (now.getMinute() / 10) * 10;
    var t = now.withMinute(m10);

    for (int i = 0; i <= MAX_STEPS; i++) {
      var tt = t.minusMinutes((long) STEP_MIN * i);
      String tm = String.format("%04d%02d%02d%02d%02d",
          tt.getYear(), tt.getMonthValue(), tt.getDayOfMonth(), tt.getHour(), tt.getMinute());
      try {
        String body = getObsData(tm, stn);
        if (hasDataLine(body)) return body;
      } catch (ResourceAccessException e) {
        // 타임아웃이면 바로 다음 슬롯 시도
        log.debug("[KMA] 타임아웃 tm={}, 다음 슬롯 시도", tm);
      } catch (Exception e) {
        log.debug("[KMA] 호출 실패 tm={}, 다음 슬롯 시도: {}", tm, e.toString());
      }
    }
    return null;
  }

  private boolean hasDataLine(String body) {
    if (body == null) return false;
    for (String line : body.split("\\R")) {
      if (!line.isBlank() && !line.startsWith("#")) return true;
    }
    return false;
  }

  /** 파서 */
  private WeatherData parse(String body) {
    if (body == null) return null;
    for (String line : body.split("\\R")) {
      if (line.isBlank() || line.startsWith("#")) continue;
      String[] c = line.trim().split("\\s+");
      if (c.length < 23) continue;
      Double temp = parseD(c[11]); // 12: TA
      Double rain = parseD(c[15]); // 16: RN
      Double snow = parseD(c[21]); // 22: SD_TOT
      return new WeatherData(temp, rain, snow);
    }
    return null;
  }

  private Double parseD(String s) {
    if (s == null || s.isBlank() || "-".equals(s)) return null;
    try {
      double v = Double.parseDouble(s);
      return (v <= -8.9) ? null : v;
    } catch (Exception e) {
      return null;
    }
  }

  /** 조건 판정 */
  private String conditionFrom(WeatherData w) {
    if (w == null) return "normal";
    if (w.snow() != null && w.snow() >= SNOW_MM) return "snowy";
    if (w.rain() != null && w.rain() >= RAIN_MM) return "rainy";
    Double t = w.temp();
    if (t != null) {
      if (t >= HOT_MIN) return "hot";
      if (t <= COLD_MAX) return "cold";
    }
    return "normal";
  }
}
