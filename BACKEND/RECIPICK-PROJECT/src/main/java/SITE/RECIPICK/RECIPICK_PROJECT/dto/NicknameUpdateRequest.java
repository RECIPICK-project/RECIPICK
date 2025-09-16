package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import lombok.Data;

/** 닉네임 변경 요청 바디 - {"newNickname":"새닉네임"} 형태로 받는다. */
@Data
public class NicknameUpdateRequest {

    private String newNickname; // 공백/길이 체크는 서비스에서 수행
}
