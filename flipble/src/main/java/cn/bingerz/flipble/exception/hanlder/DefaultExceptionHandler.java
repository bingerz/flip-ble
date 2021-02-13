package cn.bingerz.flipble.exception.hanlder;

import cn.bingerz.flipble.exception.GattException;
import cn.bingerz.flipble.exception.ConnectException;
import cn.bingerz.flipble.exception.OtherException;
import cn.bingerz.flipble.exception.ScanException;
import cn.bingerz.flipble.exception.TimeoutException;
import cn.bingerz.flipble.utils.EasyLog;

/**
 * @author hanson
 */
public class DefaultExceptionHandler extends BLEExceptionHandler {

    public DefaultExceptionHandler() {

    }

    @Override
    protected void onConnectException(ConnectException e) {
        EasyLog.e("ConnectException:" + e.getMessage());
    }

    @Override
    protected void onGattException(GattException e) {
        EasyLog.e("GattException:" + e.getMessage());
    }

    @Override
    protected void onTimeoutException(TimeoutException e) {
        EasyLog.e("TimeoutException:" + e.getMessage());
    }

    @Override
    protected void onScanException(ScanException e) {
        EasyLog.e("ScanException:" + e.getMessage());
    }

    @Override
    protected void onOtherException(OtherException e) {
        EasyLog.e("OtherException:" + e.getMessage());
    }
}
