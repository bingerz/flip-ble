package cn.bingerz.flipble.exception;

/**
 * @author hanson
 */
public class ConnectException extends BLEException {

    private int status;

    public ConnectException(int status) {
        super(ERR_CODE_GATT, "Connect Exception occurred - status=" + status);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public ConnectException setStatus(int status) {
        this.status = status;
        return this;
    }

    @Override
    public String toString() {
        return "ConnectException{" + "status=" + status + '}';
    }
}
