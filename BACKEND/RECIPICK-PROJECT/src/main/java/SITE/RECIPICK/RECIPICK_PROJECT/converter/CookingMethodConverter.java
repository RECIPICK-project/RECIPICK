package SITE.RECIPICK.RECIPICK_PROJECT.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingMethod;

/** CookingMethod Enum을 한글 description으로 DB에 저장하기 위한 컨버터 */
@Converter
public class CookingMethodConverter implements AttributeConverter<CookingMethod, String> {

    @Override
    public String convertToDatabaseColumn(CookingMethod attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDescription(); // "굽기", "끓이기" 등 한글 저장
    }

    @Override
    public CookingMethod convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }

        // 한글 description으로 enum 찾기
        for (CookingMethod method : CookingMethod.values()) {
            if (method.getDescription().equals(dbData.trim())) {
                return method;
            }
        }

        throw new IllegalArgumentException("Unknown CookingMethod: " + dbData);
    }
}
