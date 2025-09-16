// service/ImportService.java
package SITE.RECIPICK.RECIPICK_PROJECT.service;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity;
import SITE.RECIPICK.RECIPICK_PROJECT.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImportService {

  private final PostRepository postRepository;

  @Transactional
  public int importPostsFromExcel(MultipartFile file) throws Exception {
    int count = 0;

    try (var is = file.getInputStream(); var wb = new XSSFWorkbook(is)) {
      var sh = wb.getSheetAt(0);

      for (int r = 1; r <= sh.getLastRowNum(); r++) {
        var row = sh.getRow(r);
        if (row == null) continue;

        // ✅ 헬퍼 람다
        java.util.function.Function<Integer, String> S = (c) -> {
          var cell = row.getCell(c);
          if (cell == null) return null;
          switch (cell.getCellType()) {
            case NUMERIC -> {
              var d = cell.getNumericCellValue();
              if (Math.floor(d) == d) return String.valueOf((long) d);
              return String.valueOf(d);
            }
            case STRING -> { return cell.getStringCellValue().trim(); }
            case BOOLEAN -> { return String.valueOf(cell.getBooleanCellValue()); }
            case FORMULA -> { // 수식 셀은 평가 없이 문자열로
              try { return cell.getStringCellValue().trim(); }
              catch (Exception ignore) { return String.valueOf(cell.getNumericCellValue()); }
            }
            default -> { return cell.toString().trim(); }
          }
        };

        java.util.function.BiFunction<Integer, Integer, Integer> I = (c, def) -> {
          try {
            var v = S.apply(c);
            if (v == null || v.isEmpty()) return def;
            // "2.0" 형태도 들어올 수 있으니 double → int
            return (int) Math.round(Double.parseDouble(v));
          } catch (Exception e) { return def; }
        };

        // ===== 매핑 =====
        var title     = S.apply(1);
        var foodName  = S.apply(2);
        if (title == null || foodName == null) continue;

        var likeCount = I.apply(6, 0);
        var viewCount = I.apply(5, 0);
        var reportCnt = I.apply(7, 0);
        var inbun     = I.apply(12, 1);
        var level     = I.apply(13, 1);
        var time      = I.apply(14, 30);
        var imgUrl    = S.apply(16);
        var steps     = S.apply(17);
        var stepsImg  = S.apply(18);
        var isOfficial= I.apply(19, 0);

        var mth  = mapMethod(S.apply(8));
        var cat  = mapCategory(S.apply(9));
        var knd  = mapKind(S.apply(10));
        var mtrl = S.apply(11);

        var entity = PostEntity.builder()
            .userId(1)
            .title(title)
            .foodName(foodName)
            .likeCount(likeCount)
            .viewCount(viewCount)
            .reportCount(reportCnt)
            .ckgInbun(inbun)
            .ckgLevel(level)
            .ckgTime(time)
            .rcpImgUrl(imgUrl == null ? "" : imgUrl)
            .rcpSteps(steps == null ? "" : steps)
            .rcpStepsImg(stepsImg)
            .ckgMtrlCn(mtrl == null ? "" : mtrl)
            .rcpIsOfficial(isOfficial)
            .ckgMth(mth)
            .ckgCategory(cat)
            .ckgKnd(knd)
            .build();

        postRepository.save(entity);
        count++;
      }
    }
    return count;
  }

  // ===== enum 매핑 헬퍼는 클래스 레벨 메서드로 유지 =====
  private PostEntity.CookingMethod mapMethod(String s) {
    if (s == null) return PostEntity.CookingMethod.OTHER;
    for (var e : PostEntity.CookingMethod.values())
      if (e.getDescription().equals(s)) return e;
    return PostEntity.CookingMethod.OTHER;
  }
  private PostEntity.CookingCategory mapCategory(String s) {
    if (s == null) return PostEntity.CookingCategory.OTHER;
    for (var e : PostEntity.CookingCategory.values())
      if (e.getDescription().equals(s)) return e;
    return PostEntity.CookingCategory.OTHER;
  }
  private PostEntity.CookingKind mapKind(String s) {
    if (s == null) return PostEntity.CookingKind.OTHER;
    for (var e : PostEntity.CookingKind.values())
      if (e.getDescription().equals(s)) return e;
    return PostEntity.CookingKind.OTHER;
  }
}

