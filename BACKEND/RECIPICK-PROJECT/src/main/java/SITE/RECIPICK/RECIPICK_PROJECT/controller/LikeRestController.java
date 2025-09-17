package SITE.RECIPICK.RECIPICK_PROJECT.controller;


import SITE.RECIPICK.RECIPICK_PROJECT.service.LikeServiceImpl;
import SITE.RECIPICK.RECIPICK_PROJECT.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/post/{postId}/like")
public class LikeRestController {

  private final LikeServiceImpl myLikeService; // 네 서비스 그대로
  private final CurrentUser currentUser;

  @GetMapping
  public ResponseEntity<Boolean> isLiked(@PathVariable Integer postId) {
    return ResponseEntity.ok(myLikeService.isLiked(currentUser.userId(), postId));
  }

  @PostMapping
  public ResponseEntity<Void> like(@PathVariable Integer postId) {
    myLikeService.like(currentUser.userId(), postId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  public ResponseEntity<Void> unlike(@PathVariable Integer postId) {
    myLikeService.unlike(currentUser.userId(), postId);
    return ResponseEntity.noContent().build();
  }
}
