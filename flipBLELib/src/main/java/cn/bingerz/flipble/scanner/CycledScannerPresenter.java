package cn.bingerz.flipble.scanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.scanner.lescanner.LeScanCallback;
import cn.bingerz.flipble.utils.EasyLog;

/**
 * Created by hanson on 09/09/2018.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class CycledScannerPresenter implements LeScanCallback {

    private Scanner mScanner;
    private boolean mBackgroundMode;
    private List<ScanDevice> mScanDevices = new ArrayList<>();

    private long foregroundScanDuration = CentralManager.DEFAULT_FOREGROUND_SCAN_DURATION;
    private long foregroundScanInterval = CentralManager.DEFAULT_FOREGROUND_SCAN_INTERVAL;
    private long backgroundScanDuration = CentralManager.DEFAULT_BACKGROUND_SCAN_DURATION;
    private long backgroundScanInterval = CentralManager.DEFAULT_BACKGROUND_SCAN_INTERVAL;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    public CycledScannerPresenter(Scanner scanner, ScanRuleConfig config) {
        this.mScanner = scanner;
        this.mBackgroundMode = config.isBackgroundMode();
        setDuration(mBackgroundMode, config.getScanDuration());
        setInterval(mBackgroundMode, config.getScanInterval());
    }

    private void setDuration(boolean backgroundMode, long duration) {
        if (backgroundMode) {
            backgroundScanDuration = duration;
        } else {
            foregroundScanDuration = duration;
        }
    }

    private long getDuration() {
        return mBackgroundMode ? backgroundScanDuration : foregroundScanDuration;
    }

    private void setInterval(boolean backgroundMode, long interval) {
        if (backgroundMode) {
            backgroundScanInterval = interval;
        } else {
            foregroundScanInterval = interval;
        }
    }

    private long getInterval() {
        return mBackgroundMode ? backgroundScanInterval : foregroundScanInterval;
    }

    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        next(device, rssi, scanRecord);
    }

    @SuppressWarnings({"MissingPermission"})
    private void next(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null) {
            EasyLog.d("onLeScan device is null.");
            return;
        }
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

    private void delayScheduleStartScan() {
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mScanner != null) {
                        mScanner.startLeScan();
                    }
                }
            }, getInterval());
        }
    }

    private void delayScheduleStopScan() {
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mScanner != null) {
                        mScanner.stopLeScan();
                    }
                    removeHandlerMsg();
                    delayScheduleStartScan();
                }
            }, getDuration());
        }
    }

    public final void notifyScanStarted() {
        mScanDevices.clear();
        removeHandlerMsg();
        delayScheduleStopScan();
        onScanStarted();
    }

    public final void notifyScanStopped() {
        onScanFinished(mScanDevices);
    }

    public final void removeHandlerMsg() {
        mHandler.removeCallbacksAndMessages(null);
    }

    public abstract void onScanStarted();

    public abstract void onScanning(ScanDevice device);

    public abstract void onScanFinished(List<ScanDevice> scanDevices);

}
