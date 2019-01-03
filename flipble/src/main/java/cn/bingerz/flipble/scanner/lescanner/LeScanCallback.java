package cn.bingerz.flipble.scanner.lescanner;

import android.bluetooth.BluetoothDevice;

public interface LeScanCallback {
    void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord);
    void onLeScanFailed(int errorCode);
}
