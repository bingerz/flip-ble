package cn.bingerz.flipble.exception;

public class OperationException extends BLEException {

    public OperationException(String message) {
        super(ERR_CODE_OPERATION, message);
    }
}
