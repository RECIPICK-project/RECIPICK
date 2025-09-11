package SITE.RECIPICK.RECIPICK_PROJECT.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * 혜택용 회원 등급
 */
public enum UserGrade {
  BRONZE,
  SILVER,
  GOLD,
  PLATINUM,
  DIAMOND;

  @JsonCreator
  public static UserGrade from(Object raw) {
    if (raw == null) {
      throw new IllegalArgumentException("GRADE_REQUIRED");
    }
    String v = raw.toString().trim().toUpperCase();
    return switch (v) {
      case "BRONZE" -> BRONZE;
      case "SILVER" -> SILVER;
      case "GOLD" -> GOLD;
      case "PLATINUM" -> PLATINUM;
      case "DIAMOND" -> DIAMOND;
      default -> throw new IllegalArgumentException("INVALID_GRADE");
    };
  }
}

/*public void reevaluateGrade(Profile pr, int point) {
  UserGrade newGrade =
      (point >= 5000) ? UserGrade.DIAMOND :
          (point >= 2000) ? UserGrade.PLATINUM :
              (point >= 800)  ? UserGrade.GOLD :
                  (point >= 200)  ? UserGrade.SILVER :
                      UserGrade.BRONZE;
  if (pr.getGrade() != newGrade) pr.changeGrade(newGrade);
}*/
