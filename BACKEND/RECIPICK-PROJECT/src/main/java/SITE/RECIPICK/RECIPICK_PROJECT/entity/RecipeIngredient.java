package SITE.RECIPICK.RECIPICK_PROJECT.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "RECIPE_INGREDIENT")
@IdClass(RecipeIngredientId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private PostEntity postEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ing_id", insertable = false, updatable = false)
    private Ingredient ingredient;
}