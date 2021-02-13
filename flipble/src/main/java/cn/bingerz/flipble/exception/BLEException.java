package cn.bingerz.flipble.exception;

import java.io.Serializable;

/**
 * @author hanson
 */
public abstract class BLEException implements Serializable {
    private static final long serialVersionUID = 8004414918500865564L;

    public static final int ERR_CODE_TIMEOUT = 100;
    public static final int ERR_CODE_GATT = 101;
    public static final int ERR_CODE_SCAN_FAILED = 102;
    public static final int ERR_CODE_PROPERTY = 103;
    public static final int ERR_CODE_OPERATION = 104;
    public static final int ERR_CODE_OTHER = 110;


    private int code;
    private String message;

    public BLEException(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public BLEException setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public BLEException setMessage(String message) {
        this.message = message;
        return this;
    }

    @Override
    public String toString() {
        return "BLEException { " + "code=" + code + ", message='" + message + '\'' + '}';
    }
}
