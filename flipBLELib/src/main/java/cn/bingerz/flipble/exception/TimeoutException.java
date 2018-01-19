package cn.bingerz.flipble.exception;


public class TimeoutException extends BLEException {
    public TimeoutException() {
        super(ERROR_CODE_TIMEOUT, "Timeout Exception Occurred!");
    }
}
