package com.csg.airtel.aaa4j.common.constant;

public class AuthServiceConstants {
    private AuthServiceConstants() {
        // Prevent instantiation
    }

    // HTTP Headers
    public static final String HEADER_TRACE_ID = "Trace-Id";
    public static final String HEADER_USER_NAME = "User-Name";

    // Query Parameters
    public static final String PARAM_USER_NAME = "userName";
    public static final String PARAM_PASSWORD = "password";
    public static final String TRACE_ID = "traceId";

    // Default Messages
    public static final String MSG_AUTH_FAILED = "Authentication failed";
    public static final String MSG_INTERNAL_ERROR = "Internal server error occurred";
}
