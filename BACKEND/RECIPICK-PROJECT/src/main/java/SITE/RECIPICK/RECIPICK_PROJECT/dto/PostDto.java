package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import java.time.LocalDateTime;

public class PostDto {

  private Long postId;
  private String title;
  private String foodName;
  private String rcpImgUrl;
  private Integer viewCount;
  private Integer likeCount;
  private LocalDateTime createdAt;
  private Integer subScore; // 선택적 계산용 필드

  // 기본 생성자
  public PostDto() {
  }

  // 전체 필드 생성자
  public PostDto(Long postId, String title, String foodName, String rcpImgUrl,
      Integer viewCount, Integer likeCount, LocalDateTime createdAt, Integer subScore) {
    this.postId = postId;
    this.title = title;
    this.foodName = foodName;
    this.rcpImgUrl = rcpImgUrl;
    this.viewCount = viewCount;
    this.likeCount = likeCount;
    this.createdAt = createdAt;
    this.subScore = subScore;
  }

  // Getter & Setter
  public Long getPostId() {
    return postId;
  }

  public void setPostId(Long postId) {
    this.postId = postId;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getFoodName() {
    return foodName;
  }

  public void setFoodName(String foodName) {
    this.foodName = foodName;
  }

  public String getRcpImgUrl() {
    return rcpImgUrl;
  }

  public void setRcpImgUrl(String rcpImgUrl) {
    this.rcpImgUrl = rcpImgUrl;
  }

  public Integer getViewCount() {
    return viewCount;
  }

  public void setViewCount(Integer viewCount) {
    this.viewCount = viewCount;
  }

  public Integer getLikeCount() {
    return likeCount;
  }

  public void setLikeCount(Integer likeCount) {
    this.likeCount = likeCount;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public Integer getSubScore() {
    return subScore;
  }

  public void setSubScore(Integer subScore) {
    this.subScore = subScore;
  }
}
