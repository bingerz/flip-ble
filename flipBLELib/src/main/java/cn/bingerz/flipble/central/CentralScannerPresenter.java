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

import cn.bingerz.flipble.peripheral.Peripheral;
import cn.bingerz.flipble.utils.EasyLog;

/**
 * Created by hanson on 10/01/2018.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class CentralScannerPresenter implements BluetoothAdapter.LeScanCallback {

    private String[] mDeviceNames = null;
    private String mDeviceMac = null;
    private boolean mFuzzy = false;
    private List<Peripheral> mPeripheralList = new ArrayList<>();

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private long mScanTimeout = CentralManager.DEFAULT_SCAN_TIME;

    public CentralScannerPresenter(String[] names, String mac, boolean fuzzy, long timeOut) {
        this.mDeviceNames = names;
        this.mDeviceMac = mac;
        this.mFuzzy = fuzzy;
        this.mScanTimeout = timeOut;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null) {
            return;
        }

        Peripheral peripheral = new Peripheral(device, rssi, ScanRecord.parseFromBytes(scanRecord));

        onLeScan(peripheral);

        synchronized (this) {
            if (TextUtils.isEmpty(mDeviceMac) && (mDeviceNames == null || mDeviceNames.length < 1)) {
                next(peripheral);
                return;
            }

            if (!TextUtils.isEmpty(mDeviceMac)) {
                if (!mDeviceMac.equalsIgnoreCase(device.getAddress()))
                    return;
            }

            if (mDeviceNames != null && mDeviceNames.length > 0) {
                AtomicBoolean equal = new AtomicBoolean(false);
                for (String name : mDeviceNames) {
                    String remoteName = peripheral.getName();
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
            next(peripheral);
        }
    }

    private void next(Peripheral peripheral) {
        AtomicBoolean hasFound = new AtomicBoolean(false);
        for (Peripheral device : mPeripheralList) {
            if (device.getAddress().equals(peripheral.getAddress())) {
                hasFound.set(true);
            }
        }
        if (!hasFound.get()) {
            EasyLog.i("Device detected: Name: %s  Mac: %s  Rssi: %d "
                    , peripheral.getName(), peripheral.getAddress(), peripheral.getRssi());
            mPeripheralList.add(peripheral);
            onScanning(peripheral);
        }
    }

    public final void notifyScanStarted(boolean success) {
        mPeripheralList.clear();

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
        onScanFinished(mPeripheralList);
    }

    public final void removeHandlerMsg() {
        mHandler.removeCallbacksAndMessages(null);
    }

    public abstract void onScanStarted(boolean success);

    public abstract void onLeScan(Peripheral peripheral);

    public abstract void onScanning(Peripheral peripheral);

    public abstract void onScanFinished(List<Peripheral> peripheralList);

}
