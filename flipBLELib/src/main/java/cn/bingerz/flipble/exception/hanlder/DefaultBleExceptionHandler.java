package cn.bingerz.flipble.exception.hanlder;

import cn.bingerz.flipble.utils.BLELog;
import cn.bingerz.flipble.exception.GattException;
import cn.bingerz.flipble.exception.ConnectException;
import cn.bingerz.flipble.exception.NotFoundDeviceException;
import cn.bingerz.flipble.exception.OtherException;
import cn.bingerz.flipble.exception.TimeoutException;

public class DefaultBleExceptionHandler extends BLEExceptionHandler {

    private static final String TAG = "BLEExceptionHandler";

    public DefaultBleExceptionHandler() {

    }

    @Override
    protected void onConnectException(ConnectException e) {
        BLELog.e(TAG, e.getDescription());
    }

    @Override
    protected void onGattException(GattException e) {
        BLELog.e(TAG, e.getDescription());
    }

    @Override
    protected void onTimeoutException(TimeoutException e) {
        BLELog.e(TAG, e.getDescription());
    }

    @Override
    protected void onNotFoundDeviceException(NotFoundDeviceException e) {
        BLELog.e(TAG, e.getDescription());
    }

    @Override
    protected void onOtherException(OtherException e) {
        BLELog.e(TAG, e.getDescription());
    }
}
