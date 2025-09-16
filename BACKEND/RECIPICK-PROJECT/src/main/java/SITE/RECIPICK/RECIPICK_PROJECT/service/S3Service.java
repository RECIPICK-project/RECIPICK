package SITE.RECIPICK.RECIPICK_PROJECT.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class S3Service {

    private final S3Presigner s3Presigner;

    @Value("${AWS_S3_BUCKET_NAME}")
    private String bucketName;

    @Value("${AWS_REGION}")
    private String region;

    public S3Service(S3Presigner s3Presigner) {
        this.s3Presigner = s3Presigner;
    }

    /**
     * Presigned URL을 생성하여 클라이언트가 직접 S3에 업로드할 수 있도록 함
     *
     * @param fileName 원본 파일명
     * @param fileType 파일 타입 (MIME type)
     * @param folder S3 폴더명 (profile-image, recipe-thumbnails, recipe-steps-image)
     * @return PresignedUrlResponse (업로드 URL과 최종 파일 URL)
     */
    public PresignedUrlResponse generatePresignedUrl(
            String fileName, String fileType, String folder) {

        // 입력 검증
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 비어있습니다.");
        }
        if (fileType == null || fileType.trim().isEmpty()) {
            throw new IllegalArgumentException("파일 타입이 비어있습니다.");
        }
        if (folder == null || folder.trim().isEmpty()) {
            throw new IllegalArgumentException("폴더명이 비어있습니다.");
        }

        // 고유한 파일명 생성 (중복 방지)
        String uniqueFileName = generateUniqueFileName(fileName);

        // S3 키 생성 (폴더/파일명)
        String key = folder + "/" + uniqueFileName;

        // PutObjectRequest 생성
        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType(fileType)
                        .build();

        // Presigned URL 생성 (15분 만료)
        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(15))
                        .putObjectRequest(putObjectRequest)
                        .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        // 최종 파일 URL (업로드 완료 후 접근 가능한 URL)
        String fileUrl =
                String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);

        return new PresignedUrlResponse(presignedRequest.url().toString(), fileUrl, key);
    }

    /** 고유한 파일명 생성 (UUID + 원본 파일명) */
    private String generateUniqueFileName(String originalFileName) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        String extension = "";

        if (originalFileName.contains(".")) {
            extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            originalFileName = originalFileName.substring(0, originalFileName.lastIndexOf("."));
        }

        return uuid + "_" + originalFileName + extension;
    }

    /** Presigned URL 응답 DTO */
    public static class PresignedUrlResponse {
        private final String uploadUrl;
        private final String fileUrl;
        private final String key;

        public PresignedUrlResponse(String uploadUrl, String fileUrl, String key) {
            this.uploadUrl = uploadUrl;
            this.fileUrl = fileUrl;
            this.key = key;
        }

        public String getUploadUrl() {
            return uploadUrl;
        }

        public String getFileUrl() {
            return fileUrl;
        }

        public String getKey() {
            return key;
        }
    }
}
