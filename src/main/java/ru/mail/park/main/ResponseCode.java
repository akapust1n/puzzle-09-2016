package ru.mail.park.main;

public enum ResponseCode {
    @SuppressWarnings("EnumeratedConstantNamingConvention")
    OK(0, "OK"),
    PARAMETER_MISSING(1, "Required parameter is missing"),
    AUTH_ERROR(2, "Authorization error"),
    DUPLICATE_USER(3, "User is already registered"),
    DB_ERROR(8, "Error querying database");

    private int code;
    private String message;

    ResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
