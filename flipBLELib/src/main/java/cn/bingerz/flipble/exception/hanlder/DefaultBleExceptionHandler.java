package cn.bingerz.flipble.exception.hanlder;

import cn.bingerz.flipble.utils.BleLog;
import cn.bingerz.flipble.exception.GattException;
import cn.bingerz.flipble.exception.ConnectException;
import cn.bingerz.flipble.exception.NotFoundDeviceException;
import cn.bingerz.flipble.exception.OtherException;
import cn.bingerz.flipble.exception.TimeoutException;

public class DefaultBleExceptionHandler extends BleExceptionHandler {

    private static final String TAG = "BleExceptionHandler";

    public DefaultBleExceptionHandler() {

    }

    @Override
    protected void onConnectException(ConnectException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onGattException(GattException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onTimeoutException(TimeoutException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onNotFoundDeviceException(NotFoundDeviceException e) {
        BleLog.e(TAG, e.getDescription());
    }

    @Override
    protected void onOtherException(OtherException e) {
        BleLog.e(TAG, e.getDescription());
    }
}
