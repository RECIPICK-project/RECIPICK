package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostUpdateRequest {

    private String title;
    private String foodName;
    private String ckgMth;
    private String ckgCategory;
    private String ckgKnd;
    private String ckgMtrlCn;
    private Integer ckgInbun;
    private Integer ckgLevel;
    private Integer ckgTime;
    private String rcpImgUrl;
    private String rcpSteps;
    private String rcpStepsImg;
}
