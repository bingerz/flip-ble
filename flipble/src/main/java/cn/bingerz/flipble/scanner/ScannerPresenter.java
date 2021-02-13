package cn.bingerz.flipble.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.bingerz.flipble.scanner.lescanner.LeScanCallback;
import cn.bingerz.flipble.utils.EasyLog;

/**
 * @author hanson
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class ScannerPresenter implements LeScanCallback {

    private List<ScanDevice> mScanDevices = new ArrayList<>();

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        next(device, rssi, scanRecord);
    }

    @Override
    public void onLeScanFailed(int errorCode) {
        onScanFailed(errorCode);
    }

    @SuppressWarnings({"MissingPermission"})
    private void next(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null) {
            EasyLog.e("Scanner:onLeScan next device is null");
            return;
        }
        AtomicBoolean hasFound = new AtomicBoolean(false);
        for (ScanDevice scanDevice : mScanDevices) {
            if (scanDevice.getAddress().equals(device.getAddress())) {
                hasFound.set(true);
            }
        }
        if (!hasFound.get()) {
            EasyLog.i("Device detected Name=%s  Mac=%s  Rssi=%d ",
                        device.getName(), device.getAddress(), rssi);
            ScanDevice scanDevice = new ScanDevice(device, rssi, scanRecord);
            mScanDevices.add(scanDevice);
            onScanning(scanDevice);
        }
    }

    public final void notifyScanStarted() {
        mScanDevices.clear();
        onScanStarted();
    }

    public final void notifyScanStopped() {
        onScanFinished(mScanDevices);
    }

    public abstract void onScanStarted();

    public abstract void onScanning(ScanDevice device);

    public abstract void onScanFinished(List<ScanDevice> scanDevices);

    public abstract void onScanFailed(int errorCode);

}
