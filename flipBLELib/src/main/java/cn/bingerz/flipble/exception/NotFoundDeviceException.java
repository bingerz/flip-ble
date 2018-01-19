package cn.bingerz.flipble.exception;


public class NotFoundDeviceException extends BLEException {
    public NotFoundDeviceException() {
        super(ERROR_CODE_NOT_FOUND_DEVICE, "Not Found Device Exception Occurred!");
    }
}
