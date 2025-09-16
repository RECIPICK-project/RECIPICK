// controller/ImportController.java
package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.service.ImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin Import")
@RestController
@RequestMapping("/api/admin/import")
@RequiredArgsConstructor
public class ImportController {

  private final ImportService importService;

  @Operation(summary = "엑셀(.xlsx) 업로드 → post 테이블 적재")
  @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, Object> importPosts(
      @Parameter(
          description = "엑셀 파일(.xlsx)",
          required = true,
          content = @Content(
              mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
              schema = @Schema(type = "string", format = "binary")
          )
      )
      @RequestPart("file") MultipartFile file
  ) throws Exception {
    int inserted = importService.importPostsFromExcel(file);
    return Map.of("inserted", inserted);
  }
}
