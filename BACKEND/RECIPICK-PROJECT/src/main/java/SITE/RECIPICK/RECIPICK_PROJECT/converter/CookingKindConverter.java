package SITE.RECIPICK.RECIPICK_PROJECT.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingKind;

/** CookingKind Enum을 한글 description으로 DB에 저장하기 위한 컨버터 */
@Converter
public class CookingKindConverter implements AttributeConverter<CookingKind, String> {

    @Override
    public String convertToDatabaseColumn(CookingKind attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDescription(); // "과자", "국/탕" 등 한글 저장
    }

    @Override
    public CookingKind convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }

        // 한글 description으로 enum 찾기
        for (CookingKind kind : CookingKind.values()) {
            if (kind.getDescription().equals(dbData.trim())) {
                return kind;
            }
        }

        throw new IllegalArgumentException("Unknown CookingKind: " + dbData);
    }
}
