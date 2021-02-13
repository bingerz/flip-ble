package cn.bingerz.flipble.scanner;

import android.bluetooth.BluetoothAdapter;
import android.os.Handler;
import android.os.Looper;

import java.util.List;

import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.scanner.callback.ScanCallback;
import cn.bingerz.flipble.scanner.lescanner.LeScanner;

/**
 * @author hanson
 */
public abstract class Scanner {

    protected LeScanner mLeScanner;
    protected ScanCallback mScanCallback;
    protected ScannerPresenter mScannerPresenter;

    protected Handler mHandler = new Handler(Looper.getMainLooper());

    public static Scanner createScanner(boolean isCycled) {
        if (isCycled) {
            return new CycledScanner();
        } else {
            return new OnceScanner();
        }
    }

    protected void initLeScanner(ScanRuleConfig config) {
        if (mLeScanner == null) {
            BluetoothAdapter bluetoothAdapter = CentralManager.getInstance().getBluetoothAdapter();
            if (bluetoothAdapter != null) {
                mScannerPresenter = new CycledScanner.myScannerPresenter();
                mLeScanner = LeScanner.createScanner(bluetoothAdapter, config, mScannerPresenter);
            }
        } else {
            mLeScanner.updateScanRuleConfig(config);
        }
    }

    public abstract void initConfig(ScanRuleConfig config);

    public abstract void startScanner(final ScanCallback callback);

    public abstract void stopScanner();

    protected abstract void notifyScanStarted();

    protected abstract void notifyScanStopped();

    protected synchronized void startLeScanner() {
        if (mLeScanner != null) {
            mLeScanner.scanLeDevice(true);
        }
        notifyScanStarted();
    }

    protected synchronized void stopLeScanner() {
        if (mLeScanner != null) {
            mLeScanner.scanLeDevice(false);
        }
        notifyScanStopped();
    }

    public boolean isScanning() {
        return mLeScanner != null && mLeScanner.isLeScanned();
    }

    public final void removeHandlerMsg() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    protected void destroy() {
        removeHandlerMsg();
        if (mLeScanner != null) {
            mLeScanner.destroy();
            mLeScanner = null;
        }
    }

    protected class myScannerPresenter extends ScannerPresenter {

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

        @Override
        public void onScanFailed(int errorCode) {
            if (mScanCallback != null) {
                mScanCallback.onScanFailed(errorCode);
            }
        }
    }
}
