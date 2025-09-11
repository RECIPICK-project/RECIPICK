package SITE.RECIPICK.RECIPICK_PROJECT.controller;

import SITE.RECIPICK.RECIPICK_PROJECT.service.S3Service;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/s3")
@CrossOrigin(origins = "*")
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    /**
     * Presigned URL 생성 API
     * 
     * @param request 파일명, 파일타입, 폴더명을 포함한 요청
     * @return Presigned URL과 최종 파일 URL
     */
    @PostMapping("/presigned-url")
    public ResponseEntity<S3Service.PresignedUrlResponse> generatePresignedUrl(
            @RequestBody PresignedUrlRequest request) {
        
        try {
            // 입력 데이터 유효성 검사
            if (request.getFileName() == null || request.getFileName().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            if (request.getFileType() == null || request.getFileType().trim().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // 폴더 유효성 검사
            if (!isValidFolder(request.getFolder())) {
                return ResponseEntity.badRequest().build();
            }
            
            // 파일 타입 검사 (이미지만 허용)
            if (!isValidImageType(request.getFileType())) {
                return ResponseEntity.badRequest().build();
            }

            S3Service.PresignedUrlResponse response = s3Service.generatePresignedUrl(
                    request.getFileName(),
                    request.getFileType(),
                    request.getFolder()
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 허용된 폴더인지 검사
     */
    private boolean isValidFolder(String folder) {
        return folder != null && (
                folder.equals("profile-image") ||
                folder.equals("recipe-thumbnails") ||
                folder.equals("recipe-steps-image")
        );
    }

    /**
     * 허용된 이미지 타입인지 검사
     */
    private boolean isValidImageType(String fileType) {
        return fileType != null && (
                fileType.equals("image/jpeg") ||
                fileType.equals("image/jpg") ||
                fileType.equals("image/png") ||
                fileType.equals("image/webp") ||
                fileType.equals("image/gif")
        );
    }

    /**
     * Presigned URL 요청 DTO
     */
    public static class PresignedUrlRequest {
        private String fileName;
        private String fileType;
        private String folder;

        // Constructors
        public PresignedUrlRequest() {}

        public PresignedUrlRequest(String fileName, String fileType, String folder) {
            this.fileName = fileName;
            this.fileType = fileType;
            this.folder = folder;
        }

        // Getters and Setters
        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFileType() {
            return fileType;
        }

        public void setFileType(String fileType) {
            this.fileType = fileType;
        }

        public String getFolder() {
            return folder;
        }

        public void setFolder(String folder) {
            this.folder = folder;
        }
    }
}