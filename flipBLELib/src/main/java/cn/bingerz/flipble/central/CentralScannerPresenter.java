package cn.bingerz.flipble.central;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.bingerz.flipble.utils.EasyLog;

/**
 * Created by hanson on 10/01/2018.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class CentralScannerPresenter implements BluetoothAdapter.LeScanCallback {

    private String[] mDeviceNames = null;
    private String mDeviceMac = null;
    private boolean mFuzzy = false;
    private List<ScanDevice> mScanDevices = new ArrayList<>();

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private long mScanTimeout = CentralManager.DEFAULT_SCAN_TIME;

    public CentralScannerPresenter(String[] names, String mac, boolean fuzzy, long timeOut) {
        this.mDeviceNames = names;
        this.mDeviceMac = mac;
        this.mFuzzy = fuzzy;
        this.mScanTimeout = timeOut;
    }

    @SuppressWarnings({"MissingPermission"})
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null) {
            return;
        }

        synchronized (this) {
            if (TextUtils.isEmpty(mDeviceMac) && (mDeviceNames == null || mDeviceNames.length < 1)) {
                next(device, rssi, scanRecord);
                return;
            }

            if (!TextUtils.isEmpty(mDeviceMac)) {
                if (!mDeviceMac.equalsIgnoreCase(device.getAddress()))
                    return;
            }

            if (mDeviceNames != null && mDeviceNames.length > 0) {
                AtomicBoolean equal = new AtomicBoolean(false);
                for (String name : mDeviceNames) {
                    String remoteName = device.getName();
                    if (remoteName == null)
                        remoteName = "";
                    if (mFuzzy ? remoteName.contains(name) : remoteName.equals(name)) {
                        equal.set(true);
                    }
                }
                if (!equal.get()) {
                    return;
                }
            }
            next(device, rssi, scanRecord);
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void next(BluetoothDevice device, int rssi, byte[] scanRecord) {
        AtomicBoolean hasFound = new AtomicBoolean(false);
        for (ScanDevice scanDevice : mScanDevices) {
            if (scanDevice.getAddress().equals(device.getAddress())) {
                hasFound.set(true);
            }
        }
        if (!hasFound.get()) {
            EasyLog.i("Device detected: Name: %s  Mac: %s  Rssi: %d "
                    , device.getName(), device.getAddress(), rssi);
            ScanDevice scanDevice = new ScanDevice(device, rssi, scanRecord);
            mScanDevices.add(scanDevice);
            onScanning(scanDevice);
        }
    }

    public final void notifyScanStarted(boolean success) {
        mScanDevices.clear();

        removeHandlerMsg();

        if (success && mScanTimeout > 0) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (CentralManager.getInstance().isScanning()) {
                        CentralManager.getInstance().getScanner().stopLeScan();
                    }
                }
            }, mScanTimeout);
        }

        onScanStarted(success);
    }

    public final void notifyScanStopped() {
        removeHandlerMsg();
        onScanFinished(mScanDevices);
    }

    public final void removeHandlerMsg() {
        mHandler.removeCallbacksAndMessages(null);
    }

    public abstract void onScanStarted(boolean success);

    public abstract void onScanning(ScanDevice device);

    public abstract void onScanFinished(List<ScanDevice> scanDevices);

}
