package cn.bingerz.flipble.exception;

/**
 * @author hanson
 */
public class TimeoutException extends BLEException {
    public TimeoutException() {
        super(ERR_CODE_TIMEOUT, "Timeout Exception occurred!");
    }
}
