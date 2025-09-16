package SITE.RECIPICK.RECIPICK_PROJECT.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import SITE.RECIPICK.RECIPICK_PROJECT.entity.PostEntity.CookingCategory;

/** CookingCategory Enum을 한글 description으로 DB에 저장하기 위한 컨버터 */
@Converter
public class CookingCategoryConverter implements AttributeConverter<CookingCategory, String> {

    @Override
    public String convertToDatabaseColumn(CookingCategory attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDescription(); // "가공식품류", "건어물류" 등 한글 저장
    }

    @Override
    public CookingCategory convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }

        // 한글 description으로 enum 찾기
        for (CookingCategory category : CookingCategory.values()) {
            if (category.getDescription().equals(dbData.trim())) {
                return category;
            }
        }

        throw new IllegalArgumentException("Unknown CookingCategory: " + dbData);
    }
}
