package src.main.java.SITE.RECIPICK.RECIPICK_PROJECT.controller;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GPTController {
  final static String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
  final static String OPENAI_API_KEY = ""; // <<-- 여기에 실제 API 키를 넣으세요.
  final static String MODEL_NAME = "gpt-3.5-turbo"; // chat/completions 모델 사용
  final static double TEMPERATURE = 0.1;
  final static int MAX_TOKEN = 30;
  final static String systemMessageContent = "당신은 요리 전문가로서 재료의 대체품을 추천합니다. '아삭한 식감', '비슷한 맛' 등 재료의 핵심 특성을 고려하여, 재료의 원래 특징을 대체할 수 있는 다른 재료들을 찾아주세요. 답변은 항상 '단어1, 단어2, 단어3' 형식으로 제공합니다. 접속사, 조사, 마침표(.)는 사용하지 마세요.";

  @GetMapping("/api/substitute-ingredient")
  public String getSubstituteIngredient(@RequestParam String ingredientName, @RequestParam String title) {
    String PROMPT = title + "의 재료 중 " + ingredientName + "을(를) 대체할 수 있는 재료를 추천해줘.";
    String returnAiStr = openai_api_chat_worker(OPENAI_API_URL, OPENAI_API_KEY, MODEL_NAME
        , TEMPERATURE, MAX_TOKEN, systemMessageContent, PROMPT);
    return returnAiStr;
  }

  private String openai_api_chat_worker(String openaiApiUrl, String openaiApiKey, String model
      , double temperature, int maxToken, String systemMessageContent, String prompt){
    try {
      URL url = new URL(openaiApiUrl);  //URL 객체는 단순히 주소 정보만을 담고 있으며, 실제로 네트워크 연결을 수행하지는 않습니다.(통신대상을 정의)
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();  //HttpURLConnection은 그 대상과 통신하는 모든 과정을 제어합니다.
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("Authorization", "Bearer " + openaiApiKey);
      connection.setDoOutput(true);

      // 최신 API 요청 형식에 맞게 변경
      String requestData = String.format(
          "{\"model\": \"%s\", \"temperature\": %f, \"max_tokens\": %d, \"messages\": [{\"role\": \"system\", \"content\": \"%s\"},{\"role\": \"user\", \"content\": \"%s\"}]}"
          , model, temperature, maxToken, systemMessageContent, prompt
      );

      try (OutputStream os = connection.getOutputStream()) {  //일반적인 try-catch-finally 구문(소괄호없음)과 달리, try-with-resources는 try (...) 괄호 안에 선언된 자원(리소스)을 try 블록이 종료될 때 자동으로 닫아줍니다.
        byte[] input = requestData.getBytes(StandardCharsets.UTF_8);  //문자열을 바이트배열로 변환 (문자열=model,temp,maxtoken,...)
        os.write(input, 0, input.length); //input 배열의 시작부터 끝까지 모든 바이트 데이터를 os가 가리키는 네트워크 스트림에 전송
      }

      if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8
            )
        )) {  //바이트 스트림(InputStream)을 문자 스트림(Reader)으로 변환
          StringBuilder response = new StringBuilder();
          String responseLine;
          while ((responseLine = br.readLine()) != null) {  //br 객체로부터 데이터를 한 줄씩 읽어옴 -> responseLine에 대입 -> 이 값이 null(데이터스트림의 끝)이 아닐 때 실행
            response.append(responseLine.trim()); //responseLine을 StringBuilder 객체인 response의 끝에 추가
          }
          String jsonString = response.toString();
          JSONObject jsonResponse = new JSONObject(jsonString);

          // 'choices' 배열의 첫 번째 요소에 접근
          // 'message' 객체에 접근
          // 'content' 키의 값 추출
          String content = jsonResponse
              .getJSONArray("choices")
              .getJSONObject(0)
              .getJSONObject("message")
              .getString("content");

          return content; // 순수한 텍스트만 반환
        }
      } else {
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(
                connection.getErrorStream(), StandardCharsets.UTF_8
            )
        )) {
          StringBuilder errorResponse = new StringBuilder();
          String errorLine;
          while ((errorLine = br.readLine()) != null) {
            errorResponse.append(errorLine.trim());
          }
          return "[에러] 응답 코드: " + connection.getResponseCode() + ", 에러 메시지: "
              + errorResponse.toString();
        }
      }
    } catch (Exception e) {
      return "[예외 발생] " + e.getMessage();
    }
  }
}
