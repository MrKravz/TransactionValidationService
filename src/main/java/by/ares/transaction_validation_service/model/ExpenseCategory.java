package by.ares.transaction_validation_service.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ExpenseCategory {
    PRODUCT("product"),
    SERVICE("service");

    private final String code;

    ExpenseCategory(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    public static ExpenseCategory fromCode(String code) {
        for (ExpenseCategory category : values()) {
            if (category.code.equalsIgnoreCase(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown expense category: " + code);
    }
}