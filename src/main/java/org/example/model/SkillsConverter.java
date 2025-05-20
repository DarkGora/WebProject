package org.example.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter(autoApply = true)
public class SkillsConverter implements AttributeConverter<Skills, String> {
    private static final Logger log = LoggerFactory.getLogger(SkillsConverter.class);

    @Override
    public String convertToDatabaseColumn(Skills skill) {
        return skill == null ? null : skill.name();
    }

    @Override
    public Skills convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return Skills.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.warn("Некорректное значение навыка в базе данных: {}. Возвращается null.", dbData);
            return null;
        }
    }
}