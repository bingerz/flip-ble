package cn.bingerz.flipble.scanner;

import android.bluetooth.BluetoothAdapter;

import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.scanner.callback.ScanCallback;
import cn.bingerz.flipble.scanner.lescanner.LeScanCallback;
import cn.bingerz.flipble.scanner.lescanner.LeScanner;

public abstract class Scanner {
    protected LeScanner mLeScanner;
    protected ScanCallback mScanCallback;
    protected ScanState scanState = ScanState.STATE_IDLE;

    public static Scanner createScanner(boolean isCycled) {
        if (isCycled) {
            return new CycledScanner();
        } else {
            return new OnceScanner();
        }
    }

    protected void initLeScanner(ScanRuleConfig config, LeScanCallback callback) {
        if (mLeScanner == null) {
            BluetoothAdapter bluetoothAdapter = CentralManager.getInstance().getBluetoothAdapter();
            if (bluetoothAdapter != null) {
                mLeScanner = LeScanner.createScanner(bluetoothAdapter, config, callback);
            }
        }
    }

    public abstract void initConfig(ScanRuleConfig config);

    public abstract void startScan(final ScanCallback callback);

    public abstract void stopScan();

    protected abstract void notifyScanStarted();

    protected abstract void notifyScanStopped();

    protected synchronized void startLeScan() {
        if (mLeScanner != null) {
            mLeScanner.scanLeDevice(true);
        }
        scanState = ScanState.STATE_SCANNING;
        notifyScanStarted();
    }

    protected synchronized void stopLeScan() {
        if (mLeScanner != null) {
            mLeScanner.scanLeDevice(false);
        }
        scanState = ScanState.STATE_IDLE;
        notifyScanStopped();
    }

    public ScanState getScanState() {
        return scanState;
    }

    public boolean isScanning() {
        return getScanState() == ScanState.STATE_SCANNING;
    }

    public void destroy() {
        if (mLeScanner != null) {
            mLeScanner.destroy();
        }
    }
}
