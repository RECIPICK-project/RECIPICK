package SITE.RECIPICK.RECIPICK_PROJECT.dto;

public record MainPostDto(
    Integer id,
    String title,
    String foodName,
    String rcpImgUrl,
    Integer likeCount,
    Integer viewCount
) {}