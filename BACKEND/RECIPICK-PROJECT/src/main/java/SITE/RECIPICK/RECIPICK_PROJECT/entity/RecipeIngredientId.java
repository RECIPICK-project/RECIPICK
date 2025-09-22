package SITE.RECIPICK.RECIPICK_PROJECT.entity;

import java.io.Serializable;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class RecipeIngredientId implements Serializable {

    private Integer postId;
    private Integer ingId;
}
