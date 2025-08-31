package group3.recipe.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  private String email;
  private String code;
  private LocalDateTime expireAt;
  private Boolean used;
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();
 ;
}