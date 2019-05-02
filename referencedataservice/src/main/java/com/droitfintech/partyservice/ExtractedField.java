package com.droitfintech.partyservice;

/**
 * Created by barry on 8/17/16.
 */
public class ExtractedField {
    public static enum FieldType {
        STRING,
        BOOLEAN,
        OBJECT
    }
    private String fieldName;
    private FieldType fieldType;

    public ExtractedField(String fieldName, FieldType fieldType) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public FieldType getFieldType() {
        return fieldType;
    }
}
