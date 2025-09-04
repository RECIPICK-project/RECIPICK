package SITE.RECIPICK.RECIPICK_PROJECT.entity.search;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "POST")
@Getter
@Setter
public class SearchPost {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "post_id")
  private Integer postId;

  @Column(name = "user_id", nullable = false)
  private Integer userId;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "food_name", nullable = false, length = 100)
  private String foodName;

  @Column(name = "view_count")
  private Integer viewCount = 0;

  @Column(name = "like_count")
  private Integer likeCount = 0;

  @Column(name = "rcp_img_url")
  private String rcpImgUrl;

  @Column(name = "rcp_steps_img")
  private String rcpStepsImg;

  @Column(name = "created_at")
  private LocalDateTime createdAt = LocalDateTime.now();

  @Column(name = "updated_at")
  private LocalDateTime updatedAt = LocalDateTime.now();

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RecipeIngredient> recipeIngredients = new ArrayList<>();
}
