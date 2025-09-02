package SITE.RECIPICK.RECIPICK_PROJECT.entity;

/**
 * 혜택용 회원 등급
 */
public enum UserGrade {
  BRONZE,
  SILVER,
  GOLD,
  PLATINUM,
  DIAMOND
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
