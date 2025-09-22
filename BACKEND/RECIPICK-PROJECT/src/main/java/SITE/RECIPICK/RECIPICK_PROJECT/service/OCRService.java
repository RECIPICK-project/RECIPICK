package SITE.RECIPICK.RECIPICK_PROJECT.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import SITE.RECIPICK.RECIPICK_PROJECT.repository.SearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OCRService {

    // 영수증에서 제외할 키워드들 (상점명, 결제 관련 등)
    private static final Set<String> EXCLUDE_KEYWORDS =
            Set.of(
                    "편의점", "마트", "슈퍼", "상점", "할인", "적립", "포인트", "카드", "현금", "합계", "총액", "부가세", "봉투",
                    "비닐", "영수증", "거래", "결제", "원", "₩", "개", "EA", "kg", "g", "ml", "l", "매", "봉",
                    "팩");
    // 재료로 인식될 가능성이 높은 패턴들
    private static final Pattern INGREDIENT_PATTERN =
            Pattern.compile(
                    "^[가-힣]{2,8}$|"
                            + // 한글 2-8글자
                            "^[가-힣]+[0-9]*[가-힣]*$" // 한글+숫자 조합
                    );
    private final SearchRepository searchRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${naver.clova.ocr.api-url}")
    private String apiUrl;

    @Value("${naver.clova.ocr.secret-key}")
    private String secretKey;

    /** 영수증 이미지에서 재료 추출 */
    public List<String> extractIngredientsFromImage(MultipartFile imageFile) throws Exception {
        // 1. 네이버 클로바 OCR 호출
        String extractedText = callClovaOCR(imageFile);

        // 2. 텍스트에서 재료 후보 추출
        List<String> candidateIngredients = extractIngredientCandidates(extractedText);

        // 3. DB의 실제 재료와 매칭
        List<String> matchedIngredients = matchWithDatabase(candidateIngredients);

        log.info("영수증 OCR 처리 완료 - 추출된 재료: {}", matchedIngredients);

        return matchedIngredients;
    }

    /** 네이버 클로바 OCR API 호출 */
    private String callClovaOCR(MultipartFile imageFile) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-OCR-SECRET", secretKey);

        // 요청 바디 구성
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("version", "V2");
        requestBody.put("requestId", UUID.randomUUID().toString());
        requestBody.put("timestamp", System.currentTimeMillis());

        // 이미지 정보
        Map<String, Object> image = new HashMap<>();
        image.put("format", getImageFormat(imageFile.getOriginalFilename()));
        image.put("data", Base64.getEncoder().encodeToString(imageFile.getBytes()));
        image.put("name", "receipt");

        requestBody.put("images", List.of(image));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return parseOCRResponse(response.getBody());
            } else {
                throw new RuntimeException("OCR API 호출 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("네이버 클로바 OCR API 호출 중 오류 발생", e);
            throw new RuntimeException("OCR 처리 중 오류가 발생했습니다.", e);
        }
    }

    /** OCR API 응답에서 텍스트 추출 */
    private String parseOCRResponse(String responseBody) throws JsonProcessingException {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        StringBuilder extractedText = new StringBuilder();

        JsonNode images = rootNode.get("images");
        if (images != null && images.isArray()) {
            for (JsonNode image : images) {
                JsonNode fields = image.get("fields");
                if (fields != null && fields.isArray()) {
                    for (JsonNode field : fields) {
                        String text = field.get("inferText").asText();
                        extractedText.append(text).append("\n");
                    }
                }
            }
        }

        return extractedText.toString();
    }

    /** 텍스트에서 재료 후보 추출 */
    private List<String> extractIngredientCandidates(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 줄 단위로 분리하여 처리
        return Arrays.stream(text.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(this::cleanIngredientName)
                .filter(Objects::nonNull)
                .filter(this::isLikelyIngredient)
                .distinct()
                .collect(Collectors.toList());
    }

    /** 재료명 정제 (숫자, 특수문자 제거) */
    private String cleanIngredientName(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return null;
        }

        // 가격, 수량 등 숫자가 포함된 부분 제거
        String cleaned =
                rawName.replaceAll("[0-9,]+원?", "")
                        .replaceAll("[0-9]+[gkml]+", "")
                        .replaceAll("[0-9]+개", "")
                        .replaceAll("[*×]", "")
                        .replaceAll("[()\\[\\]]", "")
                        .trim();

        // 너무 짧거나 긴 경우 제외
        if (cleaned.length() < 2 || cleaned.length() > 10) {
            return null;
        }

        return cleaned;
    }

    /** 재료일 가능성 판단 */
    private boolean isLikelyIngredient(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        // 제외 키워드 체크
        for (String keyword : EXCLUDE_KEYWORDS) {
            if (text.contains(keyword)) {
                return false;
            }
        }

        // 패턴 매칭
        return INGREDIENT_PATTERN.matcher(text).matches();
    }

    /** DB의 실제 재료와 매칭 */
    private List<String> matchWithDatabase(List<String> candidates) {
        List<String> matchedIngredients = new ArrayList<>();

        for (String candidate : candidates) {
            try {
                // 정확히 일치하는 재료 검색
                List<String> exactMatches = searchRepository.findIngredientsByKeyword(candidate, 1);
                if (!exactMatches.isEmpty()) {
                    matchedIngredients.add(exactMatches.get(0));
                    continue;
                }

                // 부분 일치 검색
                List<String> partialMatches =
                        searchRepository.findIngredientsByKeyword(
                                candidate.substring(0, Math.min(candidate.length(), 3)), 3);
                for (String match : partialMatches) {
                    if (match.contains(candidate) || candidate.contains(match)) {
                        matchedIngredients.add(match);
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("재료 매칭 중 오류: {}", candidate, e);
            }
        }

        return matchedIngredients.stream().distinct().collect(Collectors.toList());
    }

    /** 이미지 파일 형식 추출 */
    private String getImageFormat(String filename) {
        if (filename == null) {
            return "jpg";
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        switch (extension) {
            case "jpeg":
            case "jpg":
                return "jpg";
            case "png":
                return "png";
            default:
                return "jpg";
        }
    }

    /** OCR 결과 검증 및 신뢰도 체크 */
    public boolean validateOCRResult(String extractedText) {
        if (extractedText == null || extractedText.trim().length() < 10) {
            return false;
        }

        // 영수증 특성 키워드 존재 여부 체크
        String[] receiptKeywords = {"합계", "총액", "카드", "현금", "영수증"};
        long keywordCount =
                Arrays.stream(receiptKeywords)
                        .mapToLong(keyword -> extractedText.contains(keyword) ? 1 : 0)
                        .sum();

        return keywordCount >= 1; // 최소 1개 이상의 영수증 키워드가 있어야 함
    }
}
