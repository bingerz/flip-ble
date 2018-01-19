package cn.bingerz.flipble.exception;


public class OtherException extends BLEException {
    public OtherException(String description) {
        super(ERROR_CODE_OTHER, description);
    }
}
