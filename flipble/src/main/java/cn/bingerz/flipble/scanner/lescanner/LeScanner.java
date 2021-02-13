package cn.bingerz.flipble.scanner.lescanner;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.MainThread;
import android.support.annotation.WorkerThread;

import cn.bingerz.flipble.scanner.ScanRuleConfig;
import cn.bingerz.flipble.utils.EasyLog;

/**
 * @author hanson
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class LeScanner {

    private final Handler mScanHandler;
    private final HandlerThread mScanThread;
    private final BluetoothAdapter mBluetoothAdapter;
    protected final LeScanCallback mLeScanCallback;

    protected ScanRuleConfig mScanRuleConfig;
    private LeScanState mScanState = LeScanState.STATE_IDLE;

    protected LeScanner(BluetoothAdapter bluetoothAdapter, ScanRuleConfig config, LeScanCallback callback) {
        mBluetoothAdapter = bluetoothAdapter;
        mScanRuleConfig = config;
        mLeScanCallback = callback;

        mScanThread = new HandlerThread("LeScannerThread");
        mScanThread.start();
        mScanHandler = new Handler(mScanThread.getLooper());
        EasyLog.v("Scan thread starting");
    }

    public static LeScanner createScanner(BluetoothAdapter bluetoothAdapter, ScanRuleConfig config, LeScanCallback callback) {
        boolean useAndroidLScanner = false;
        boolean useAndroidOScanner = false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            EasyLog.w("Create scanner fail, Not supported prior to API 18.");
            return null;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            EasyLog.i("This is pre Android 5.0. We are using old scanning APIs");
            useAndroidLScanner = false;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            EasyLog.i("This is Android 5.0. We are using new scanning APIs");
            useAndroidLScanner = true;
        } else {
            EasyLog.i("Using Android O scanner");
            useAndroidOScanner = true;
        }

        if (useAndroidOScanner) {
            return new LeScannerForAndroidO(bluetoothAdapter, config, callback);
        } else if (useAndroidLScanner) {
            return new LeScannerForLollipop(bluetoothAdapter, config, callback);
        } else {
            return new LeScannerForJellyBeanMr2(bluetoothAdapter, config, callback);
        }
    }

    @MainThread
    public void scanLeDevice(boolean enable) {
        if (!isBluetoothOn()) {
            EasyLog.w("Not %s scan, bluetooth is turn off", enable ? "starting" : "stopping");
            return;
        }
        if (enable) {
            if (isLeScanStarting()) {
                EasyLog.w("LeScanner is preparing scan.");
            } else if (isLeScanned()){
                EasyLog.w("LeScanner is already scan.");
            } else {
                mScanState = LeScanState.STATE_SCANNING;
                try {
                    startLeScanner();
                } catch (Exception e) {
                    EasyLog.e(e, "Exception starting scan. Perhaps Bluetooth is disabled or unavailable?");
                }
            }
        } else {
            if (isLeScanStarting()) {
                removeScanHandlerMessages();
                mScanState = LeScanState.STATE_IDLE;
            } else if (isLeScanned()) {
                stopLeScanner();
            }
        }
    }

    @MainThread
    public void destroy() {
        EasyLog.v("LeScanner Destroying");
        scanLeDevice(false);
        postToWorkerThread(false, new Runnable() {
            @WorkerThread
            @Override
            public void run() {
                EasyLog.v("Scan thread quitting");
                mScanThread.quit();
            }
        });
    }

    private void removeScanHandlerMessages() {
        if (mScanHandler != null) {
            mScanHandler.removeCallbacksAndMessages(null);
        }
    }

    protected void postToWorkerThread(boolean isRemovePending, Runnable r) {
        if (mScanHandler != null) {
            if (isRemovePending) {
                removeScanHandlerMessages();
            }
            mScanHandler.post(r);
        }
    }

    protected abstract void startLeScanner();

    protected abstract void stopLeScanner();

    protected BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }

    @SuppressLint("MissingPermission")
    protected boolean isBluetoothOn() {
        return getBluetoothAdapter() != null && getBluetoothAdapter().isEnabled();
    }

    public void updateScanRuleConfig(ScanRuleConfig config) {
        EasyLog.v("LeScanner Update scanRuleConfig");
        mScanRuleConfig = config;
    }

    protected void setScanState(LeScanState newState) {
        mScanState = newState;
    }

    public LeScanState getScanState() {
        return mScanState;
    }

    protected boolean isLeScanStarting() {
        return mScanState == LeScanState.STATE_SCANNING;
    }

    public boolean isLeScanned() {
        return mScanState == LeScanState.STATE_SCANNED;
    }
}
