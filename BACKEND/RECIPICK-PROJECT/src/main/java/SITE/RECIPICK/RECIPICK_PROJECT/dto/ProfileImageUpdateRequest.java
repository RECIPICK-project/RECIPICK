package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileImageUpdateRequest {

    private String profileImg; // 예: "/img/new.png" or S3 URL

    public String getProfileImg() {
        return profileImg;
    }

    public void setProfileImg(String v) {
        this.profileImg = v;
    }
}
