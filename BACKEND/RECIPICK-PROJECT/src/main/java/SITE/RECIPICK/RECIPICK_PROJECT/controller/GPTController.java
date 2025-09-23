package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
public class GPTController {

  final static String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
  final static String MODEL_NAME = "gpt-3.5-turbo";
  final static double TEMPERATURE = 0.1;
  final static int MAX_TOKEN = 50; // 토큰 수를 조금 늘림
  final static String systemMessageContent = "당신은 식재료 대체 추천 AI입니다. 당신의 유일한 임무는 사용자가 요청한 재료를 대체할 수 있는 다른 재료와 그 양을 쉼표(,)로 구분하여 나열하는 것입니다. 절대로 설명, 문장, 인사, 마침표를 포함하지 마세요. 중복 재료를 알려주지 마세요. 오직 '재료1 양, 재료2 양, 재료3 양' 형식으로만 응답해야 합니다. 예시: 사용자가 '상추 1장'을 요청하면, 당신의 응답은 '양상추 2장, 치커리 1장, 로메인 1.5장' 이어야 합니다.";
  @Value("${openai.api.key:}")
  private String openaiApiKey;

  @GetMapping("/api/substitute-ingredient")
  public ResponseEntity<String> getSubstituteIngredient(
      @RequestParam String ingredientName,
      @RequestParam String amount,
      @RequestParam String title) {

    try {
      // API KEY 확인
      if (openaiApiKey == null || openaiApiKey.isEmpty()) {
        System.err.println("OpenAI API 키가 설정되지 않았습니다. .env 파일을 확인하세요.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("API 키 설정 오류");
      }

      System.out.println("재료 대체 요청: " + ingredientName + " (레시피: " + title + ")");

      String amountText =
          (amount == null || amount.isEmpty() || amount.equals("undefined")) ? "" : " " + amount;
      String PROMPT = title + "의 재료 중 " + ingredientName + amount + "을(를) 대체할 수 있는 재료를 추천해줘.";
      String returnAiStr = openai_api_chat_worker(OPENAI_API_URL, openaiApiKey, MODEL_NAME,
          TEMPERATURE, MAX_TOKEN, systemMessageContent, PROMPT);

      // 에러 메시지 체크
      if (returnAiStr.startsWith("[에러]") || returnAiStr.startsWith("[예외 발생]")) {
        System.err.println("OpenAI API 오류: " + returnAiStr);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("AI 서비스 오류");
      }

      System.out.println("AI 응답: " + returnAiStr);
      return ResponseEntity.ok(returnAiStr);

    } catch (Exception e) {
      System.err.println("서버 오류: " + e.getMessage());
      e.printStackTrace();
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("서버 내부 오류");
    }
  }

  private String openai_api_chat_worker(String openaiApiUrl, String apiKey, String model,
      double temperature, int maxToken, String systemMessageContent, String prompt) {
    try {
      URL url = new URL(openaiApiUrl);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Authorization", "Bearer " + apiKey);
      connection.setDoOutput(true);
      connection.setConnectTimeout(15000); // 15초 타임아웃
      connection.setReadTimeout(30000); // 30초 읽기 타임아웃

      // JSON 문자열에서 특수문자 이스케이프 처리
      String escapedSystemMessage = systemMessageContent.replace("\"", "\\\"").replace("\n", "\\n");
      String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", "\\n");

      String requestData = String.format(
          "{\"model\": \"%s\", \"temperature\": %.1f, \"max_tokens\": %d, \"messages\": [{\"role\": \"system\", \"content\": \"%s\"},{\"role\": \"user\", \"content\": \"%s\"}]}",
          model, temperature, maxToken, escapedSystemMessage, escapedPrompt
      );

      System.out.println("OpenAI API 요청 전송중...");

      try (OutputStream os = connection.getOutputStream()) {
        byte[] input = requestData.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }

      int responseCode = connection.getResponseCode();
      System.out.println("OpenAI API 응답 코드: " + responseCode);

      if (responseCode == HttpURLConnection.HTTP_OK) {
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
          StringBuilder response = new StringBuilder();
          String responseLine;
          while ((responseLine = br.readLine()) != null) {
            response.append(responseLine.trim());
          }
          String jsonString = response.toString();
          JSONObject jsonResponse = new JSONObject(jsonString);

          String content = jsonResponse
              .getJSONArray("choices")
              .getJSONObject(0)
              .getJSONObject("message")
              .getString("content");

          return content.trim();
        }
      } else {
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
          StringBuilder errorResponse = new StringBuilder();
          String errorLine;
          while ((errorLine = br.readLine()) != null) {
            errorResponse.append(errorLine.trim());
          }
          return "[에러] 응답 코드: " + responseCode + ", 에러 메시지: " + errorResponse;
        }
      }
    } catch (Exception e) {
      return "[예외 발생] " + e.getMessage();
    }
  }
}