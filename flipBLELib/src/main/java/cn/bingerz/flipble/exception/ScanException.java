package cn.bingerz.flipble.exception;

/**
 * Created by hanson on 2018/3/14.
 */

public class ScanException extends BLEException {

    public ScanException(String message) {
        super(ERR_CODE_SCAN_FAILED, message);
    }
}
