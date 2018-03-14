package cn.bingerz.flipble.exception;


public class OtherException extends BLEException {

    public OtherException(String message) {
        super(ERR_CODE_OTHER, message);
    }
}
