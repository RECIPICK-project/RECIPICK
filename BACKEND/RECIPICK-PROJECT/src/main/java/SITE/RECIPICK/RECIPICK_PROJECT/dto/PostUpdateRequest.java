package SITE.RECIPICK.RECIPICK_PROJECT.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostUpdateRequest {

    private String title;
    private String foodName;
    private String ckgMth;
    private String ckgCategory;
    private String ckgKnd;
    private List<String> ckgMtrlCn;
    private Integer ckgInbun;
    private Integer ckgLevel;
    private Integer ckgTime;
    private String rcpImgUrl;
    private List<String> rcpSteps;
    private List<String> rcpStepsImg;

    private List<String> ingredientNames;
    private List<String> ingredientQuantities;
    private List<String> ingredientUnits;
}