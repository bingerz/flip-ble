package cn.bingerz.flipble.exception.hanlder;

import cn.bingerz.flipble.exception.BLEException;
import cn.bingerz.flipble.exception.GattException;
import cn.bingerz.flipble.exception.ConnectException;
import cn.bingerz.flipble.exception.OtherException;
import cn.bingerz.flipble.exception.ScanException;
import cn.bingerz.flipble.exception.TimeoutException;

/**
 * @author hanson
 */
public abstract class BLEExceptionHandler {

    public BLEExceptionHandler handleException(BLEException exception) {

        if (exception != null) {
            if (exception instanceof ConnectException) {
                onConnectException((ConnectException) exception);
            } else if (exception instanceof GattException) {
                onGattException((GattException) exception);
            } else if (exception instanceof TimeoutException) {
                onTimeoutException((TimeoutException) exception);
            } else if (exception instanceof ScanException) {
                onScanException((ScanException) exception);
            } else {
                onOtherException((OtherException) exception);
            }
        }
        return this;
    }

    /**
     * connect failed
     */
    protected abstract void onConnectException(ConnectException e);

    /**
     * gatt error status
     */
    protected abstract void onGattException(GattException e);

    /**
     * operation timeout
     */
    protected abstract void onTimeoutException(TimeoutException e);

    /**
     * scan device failure
     */
    protected abstract void onScanException(ScanException e);

    /**
     * other exceptions
     */
    protected abstract void onOtherException(OtherException e);
}
