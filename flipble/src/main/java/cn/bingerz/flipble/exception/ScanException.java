package cn.bingerz.flipble.exception;

/**
 * @author hanson
 */
public class ScanException extends BLEException {

    public ScanException(String message) {
        super(ERR_CODE_SCAN_FAILED, message);
    }
}
