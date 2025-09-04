package SITE.RECIPICK.RECIPICK_PROJECT.entity.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "INGREDIENT")
@Getter
@Setter
public class Ingredient {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ing_id")
  private Integer ingId;

  @Column(name = "name", nullable = false, unique = true, length = 100)
  private String name;

  @Column(name = "sort", length = 50)
  private String sort;
}
