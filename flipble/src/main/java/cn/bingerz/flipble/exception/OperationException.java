package cn.bingerz.flipble.exception;

/**
 * @author hanson
 */
public class OperationException extends BLEException {

    public OperationException(String message) {
        super(ERR_CODE_OPERATION, message);
    }
}
