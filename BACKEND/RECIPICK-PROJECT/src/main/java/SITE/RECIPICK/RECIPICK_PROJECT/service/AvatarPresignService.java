package SITE.RECIPICK.RECIPICK_PROJECT.service;

import java.net.URL;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

//프로필 편집에서 사진 업로드할때 presign url을 생성해주는 서비스
@Service
@RequiredArgsConstructor
public class AvatarPresignService {

  private final S3Presigner presigner;

  @Value("${AWS_S3_BUCKET_NAME}")
  private String bucket;

  @Value("${AWS_REGION}")
  private String region;

  public Map<String, String> createPutUrl(Integer userId, String filename, String contentType) {
    String safeName = filename == null ? "file"
        : filename.replaceAll("[^A-Za-z0-9._-]", "_");

    // 키 규칙: profile-avatars/{userId}_{filename}
    String key = "profile-avatars/" + userId + "_" + safeName;

    PutObjectRequest putReq = PutObjectRequest.builder()
        .bucket(bucket)
        .key(key)
        .contentType(contentType == null || contentType.isBlank()
            ? "application/octet-stream" : contentType)
        .build();

    PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
        .putObjectRequest(putReq)
        .signatureDuration(Duration.ofMinutes(10))
        .build();

    PresignedPutObjectRequest presigned = presigner.presignPutObject(presignReq);
    URL putUrl = presigned.url();

    String publicUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;

    return Map.of(
        "putUrl", putUrl.toString(),
        "publicUrl", publicUrl
    );
  }
}
