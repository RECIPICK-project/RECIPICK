package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PostdetailController {

  @GetMapping("/post_detail/{id}")
  public String redirectPostDetail(@PathVariable("id") Integer id) {
    return "redirect:/pages/post_detail.html?postId=" + id;
  }

  @GetMapping("/recipe_detail/{id}")
  public String redirectRecipeDetail(@PathVariable("id") Integer id) {
    return "redirect:/pages/post_detail.html?postId=" + id;
  }

}
