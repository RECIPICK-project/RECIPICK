package SITE.RECIPICK.RECIPICK_PROJECT.service.search;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.search.Ingredient;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.search.IngredientRepository;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class OCRService {

  // 불필요한 패턴 (영수증 잡음 제거용)
  private static final List<String> NOISE_PATTERNS = Arrays.asList(
      "원", "개", "kg", "g", "ml", "L", "봉지", "포", "팩", "병",
      "할인", "적립", "포인트", "카드", "현금", "합계", "총액",
      "0", "1", "2", "3", "4", "5", "6", "7", "8", "9"
  );

  private static final int MIN_INGREDIENT_LENGTH = 2;

  private final IngredientRepository ingredientRepository;

  // tessdata 경로 (resources 아래 경로 사용)
  @Value("${tesseract.datapath}")
  private String tesseractDataPath;

  @Value("${tesseract.language}")
  private String tesseractLanguage;

  /**
   * 업로드된 이미지에서 재료 추출
   */
  public List<String> extractIngredientsFromImage(MultipartFile imageFile)
      throws IOException, TesseractException {

    log.info("이미지에서 재료 추출 시작: {}", imageFile.getOriginalFilename());

    // 1. 이미지 전처리
    BufferedImage processedImage = preprocessImage(imageFile);

    // 2. OCR 실행
    String extractedText = performOCR(processedImage);
    log.info("추출된 원본 텍스트: {}", extractedText);

    // 라인별 로그 출력
    for (String line : extractedText.split("\\r?\\n")) {
      log.info("OCR 라인: [{}]", line);
    }

    // 3. 후보 재료 추출
    List<String> candidateIngredients = extractCandidateIngredients(extractedText);
    log.info("재료 후보들: {}", candidateIngredients);

    // 4. DB 매칭
    List<String> matchedIngredients = matchIngredientsInDatabase(candidateIngredients);
    log.info("매칭된 재료들: {}", matchedIngredients);

    return matchedIngredients;
  }

  /**
   * 이미지 전처리
   */
  private BufferedImage preprocessImage(MultipartFile imageFile) throws IOException {
    BufferedImage originalImage = ImageIO.read(imageFile.getInputStream());

    // 1. 그레이스케일
    BufferedImage grayImage = convertToGrayscale(originalImage);

    // 2. 대비 향상
    BufferedImage enhancedImage = enhanceContrast(grayImage);

    // 3. 이진화
    BufferedImage binarizedImage = binarizeImage(enhancedImage);

    return binarizedImage;
  }

  private BufferedImage convertToGrayscale(BufferedImage originalImage) {
    BufferedImage grayImage = new BufferedImage(
        originalImage.getWidth(),
        originalImage.getHeight(),
        BufferedImage.TYPE_BYTE_GRAY
    );
    Graphics2D g2d = grayImage.createGraphics();
    g2d.drawImage(originalImage, 0, 0, null);
    g2d.dispose();
    return grayImage;
  }

  private BufferedImage enhanceContrast(BufferedImage image) {
    BufferedImage enhancedImage = new BufferedImage(
        image.getWidth(),
        image.getHeight(),
        BufferedImage.TYPE_BYTE_GRAY
    );

    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        int rgb = image.getRGB(x, y);
        int gray = (rgb >> 16) & 0xFF;
        int enhanced = Math.min(255, Math.max(0, (int) (1.5 * gray - 50)));
        int newRgb = (enhanced << 16) | (enhanced << 8) | enhanced;
        enhancedImage.setRGB(x, y, newRgb);
      }
    }
    return enhancedImage;
  }

  private BufferedImage binarizeImage(BufferedImage image) {
    BufferedImage binarized = new BufferedImage(
        image.getWidth(),
        image.getHeight(),
        BufferedImage.TYPE_BYTE_BINARY
    );
    Graphics2D g2d = binarized.createGraphics();
    g2d.drawImage(image, 0, 0, null);
    g2d.dispose();
    return binarized;
  }

  /**
   * OCR 실행
   */
  private String performOCR(BufferedImage image) throws TesseractException {
    ITesseract tesseract = new Tesseract();
    try {
      tesseract.setDatapath(tesseractDataPath);
      tesseract.setLanguage(tesseractLanguage);
      tesseract.setPageSegMode(1);
      tesseract.setOcrEngineMode(1);

      String result = tesseract.doOCR(image);
      return result != null ? result.trim() : "";

    } catch (TesseractException e) {
      log.error("Tesseract OCR 실행 중 오류 발생", e);
      throw e;
    }
  }

  /**
   * 텍스트에서 재료 후보 추출
   */
  private List<String> extractCandidateIngredients(String text) {
    if (text == null || text.trim().isEmpty()) {
      return new ArrayList<>();
    }

    List<String> candidates = new ArrayList<>();
    String[] lines = text.split("[\\n\\r]+");

    for (String line : lines) {
      String[] words = line.split("\\s+");
      for (String word : words) {
        String cleanWord = cleanWord(word);
        if (isValidIngredientCandidate(cleanWord)) {
          candidates.add(cleanWord);
        }
      }

      String cleanLine = cleanWord(line);
      if (isValidIngredientCandidate(cleanLine) && cleanLine.length() <= 20) {
        candidates.add(cleanLine);
      }
    }
    return candidates.stream().distinct().collect(Collectors.toList());
  }

  private String cleanWord(String word) {
    if (word == null) {
      return "";
    }
    String cleaned = word.replaceAll("[^가-힣a-zA-Z\\s]", "").trim();
    return cleaned.replaceAll("\\s+", " ");
  }

  private boolean isValidIngredientCandidate(String word) {
    if (word == null || word.trim().length() < MIN_INGREDIENT_LENGTH) {
      return false;
    }
    for (String noise : NOISE_PATTERNS) {
      if (word.toLowerCase().contains(noise.toLowerCase())) {
        return false;
      }
    }
    if (word.length() > 15) {
      return false;
    }
    return !Pattern.matches("^\\d+$", word);
  }

  /**
   * DB 매칭 (부분 매칭 강화)
   */
  private List<String> matchIngredientsInDatabase(List<String> candidates) {
    List<String> matchedIngredients = new ArrayList<>();

    for (String candidate : candidates) {
      List<Ingredient> dbResults = ingredientRepository.findTop10ByNameContainingIgnoreCase(
          candidate);
      matchedIngredients.addAll(dbResults.stream()
          .map(Ingredient::getName)
          .collect(Collectors.toList()));
    }

    return matchedIngredients.stream().distinct().collect(Collectors.toList());
  }
}
