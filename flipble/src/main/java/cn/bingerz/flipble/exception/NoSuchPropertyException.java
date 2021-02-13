package cn.bingerz.flipble.exception;

/**
 * @author hanson
 */
public class NoSuchPropertyException extends BLEException {

    public NoSuchPropertyException(String message) {
        super(ERR_CODE_PROPERTY, message);
    }
}
