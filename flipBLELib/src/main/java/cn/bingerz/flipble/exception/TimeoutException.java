package cn.bingerz.flipble.exception;


public class TimeoutException extends BLEException {
    public TimeoutException() {
        super(ERR_CODE_TIMEOUT, "Timeout Exception occurred!");
    }
}
