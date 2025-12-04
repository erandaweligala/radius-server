package com.csg.airtel.aaa4j.common.constant;
/**
 * Enum to define standard response codes and descriptions for the application.
 */
public enum ResponseCodeEnum {

    // General exception layer error
    EXCEPTION_CONTROLLER_LAYER("E1000", "Exception Controller Layer Error"),
    EXCEPTION_SERVICE_LAYER("E1001", "Exception Service Layer Error"),
    EXCEPTION_DATABASE_LAYER("E1002", "Exception in Database Layer Error"),

    // Authentication errors
    USER_NOT_FOUND("E2001", "User Not Found");

    private final String code;
    private final String description;

    ResponseCodeEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Returns the response code.
     */
    public String code() {
        return code;
    }

    /**
     * Returns the description of the response code.
     */
    public String description() {
        return description;
    }
}
