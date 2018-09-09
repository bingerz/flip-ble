package cn.bingerz.flipble.central;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.os.Build;

import java.util.List;

import cn.bingerz.flipble.scanner.callback.ScanCallback;
import cn.bingerz.flipble.scanner.lescanner.LeScanner;
import cn.bingerz.flipble.scanner.ScanDevice;
import cn.bingerz.flipble.scanner.ScanRuleConfig;

/**
 * Created by hanson on 10/01/2018.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CentralScanner {

    private LeScanner mLeScanner;
    private ScanCallback mScanCallback;
    private CentralScannerPresenter mScannerPresenter;
    private CentralScanState scanState = CentralScanState.STATE_IDLE;

    public static CentralScanner getInstance() {
        return CentralScannerHolder.sCentralScanner;
    }

    private static class CentralScannerHolder {
        private static final CentralScanner sCentralScanner = new CentralScanner();
    }

    public void scan(ScanRuleConfig config, final ScanCallback callback) {
        if (config == null) {
            throw new IllegalArgumentException("ScanRuleConfig is null.");
        }
        mScanCallback = callback;
        mLeScanner = getLeScanner(config);
        startLeScan();
    }

    private LeScanner getLeScanner(ScanRuleConfig config) {
        if (mLeScanner == null) {
            BluetoothAdapter bluetoothAdapter = CentralManager.getInstance().getBluetoothAdapter();
            if (bluetoothAdapter != null) {
                mScannerPresenter = new myScannerPresenter(config);
                mLeScanner = LeScanner.createScanner(bluetoothAdapter, config, mScannerPresenter);
            }
        }
        return mLeScanner;
    }

    @SuppressWarnings({"MissingPermission"})
    private synchronized void startLeScan() {
        if (mLeScanner != null) {
            mLeScanner.scanLeDevice(true);
        }
        scanState = CentralScanState.STATE_SCANNING;
        if (mScannerPresenter != null) {
            mScannerPresenter.notifyScanStarted();
        }
    }

    public synchronized void stopLeScan() {
        if (mLeScanner != null) {
            mLeScanner.scanLeDevice(false);
        }
        scanState = CentralScanState.STATE_IDLE;
        if (mScannerPresenter != null) {
            mScannerPresenter.notifyScanStopped();
        }
    }

    public CentralScanState getScanState() {
        return scanState;
    }

    public void destroy() {
        if (mLeScanner != null) {
            mLeScanner.destroy();
        }
    }

    private class myScannerPresenter extends CentralScannerPresenter {

        public myScannerPresenter(ScanRuleConfig config) {
            super(config);
        }

        @Override
        public void onScanStarted() {
            if (mScanCallback != null) {
                mScanCallback.onScanStarted();
            }
        }

        @Override
        public void onScanning(ScanDevice result) {
            if (mScanCallback != null) {
                mScanCallback.onScanning(result);
            }
        }

        @Override
        public void onScanFinished(List<ScanDevice> scanResultList) {
            if (mScanCallback != null) {
                mScanCallback.onScanFinished(scanResultList);
            }
        }
    }
}
