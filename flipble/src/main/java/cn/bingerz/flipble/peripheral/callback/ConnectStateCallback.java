package cn.bingerz.flipble.peripheral.callback;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCallback;
import android.os.Build;

import cn.bingerz.flipble.peripheral.Peripheral;
import cn.bingerz.flipble.exception.BLEException;

/**
 * Created by hanson on 09/01/2018.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class ConnectStateCallback extends BluetoothGattCallback {

    public abstract void onStartConnect();

    public abstract void onConnectFail(BLEException exception);

    public abstract void onConnectSuccess(String address, int status);

    public abstract void onDisConnected(boolean isActiveDisConnected, String address, int status);

}
