package cn.bingerz.flipble.callback;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCallback;
import android.os.Build;

import cn.bingerz.flipble.bluetoothle.Peripheral;
import cn.bingerz.flipble.exception.BleException;

/**
 * Created by hanson on 09/01/2018.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class ConnectionStateCallback extends BluetoothGattCallback {

    public abstract void onStartConnect();

    public abstract void onConnectFail(BleException exception);

    public abstract void onConnectSuccess(Peripheral peripheral, int status);

    public abstract void onDisConnected(boolean isActiveDisConnected, Peripheral peripheral, int status);

}
