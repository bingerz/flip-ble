package cn.bingerz.flipble.exception;

/**
 * @author hanson
 */
public class GattException extends BLEException {

    private int status;

    public GattException(int status) {
        super(ERR_CODE_GATT, "Gatt Exception occurred - status=" + status);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public GattException setStatus(int status) {
        this.status = status;
        return this;
    }

    @Override
    public String toString() {
        return "GattException{" + "status=" + status + "} " + super.toString();
    }
}
