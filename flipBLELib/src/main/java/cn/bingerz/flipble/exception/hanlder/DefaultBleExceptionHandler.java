package cn.bingerz.flipble.exception.hanlder;

import cn.bingerz.flipble.exception.GattException;
import cn.bingerz.flipble.exception.ConnectException;
import cn.bingerz.flipble.exception.NotFoundDeviceException;
import cn.bingerz.flipble.exception.OtherException;
import cn.bingerz.flipble.exception.TimeoutException;
import cn.bingerz.flipble.utils.EasyLog;

public class DefaultBleExceptionHandler extends BLEExceptionHandler {

    public DefaultBleExceptionHandler() {

    }

    @Override
    protected void onConnectException(ConnectException e) {
        EasyLog.e(e.getDescription());
    }

    @Override
    protected void onGattException(GattException e) {
        EasyLog.e(e.getDescription());
    }

    @Override
    protected void onTimeoutException(TimeoutException e) {
        EasyLog.e(e.getDescription());
    }

    @Override
    protected void onNotFoundDeviceException(NotFoundDeviceException e) {
        EasyLog.e(e.getDescription());
    }

    @Override
    protected void onOtherException(OtherException e) {
        EasyLog.e(e.getDescription());
    }
}
