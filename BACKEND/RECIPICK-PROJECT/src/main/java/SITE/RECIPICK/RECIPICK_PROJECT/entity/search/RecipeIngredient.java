package SITE.RECIPICK.RECIPICK_PROJECT.entity.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "RECIPE_INGREDIENT")
@IdClass(RecipeIngredientId.class)
@Getter
@Setter
public class RecipeIngredient {

  @Id
  @Column(name = "post_id")
  private Integer postId;

  @Id
  @Column(name = "ing_id")
  private Integer ingId;

  @Column(name = "amount")
  private String amount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", insertable = false, updatable = false)
  private SearchPost searchPost;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ing_id", insertable = false, updatable = false)
  private Ingredient ingredient;
}
