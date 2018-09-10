package cn.bingerz.flipble.scanner;

import android.annotation.TargetApi;
import android.os.Build;

import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.scanner.callback.ScanCallback;

/**
 * Created by hanson on 10/01/2018.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class OnceScanner extends Scanner {

    private long mScanDuration = CentralManager.DEFAULT_BACKGROUND_SCAN_DURATION;

    @Override
    public void initConfig(ScanRuleConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ScanRuleConfig is null.");
        }
        long duration = config.getScanDuration();
        if (duration > 0) {
            mScanDuration = duration;
        }
        initLeScanner(config);
    }

    @Override
    public void startScan(final ScanCallback callback) {
        mScanCallback = callback;
        startLeScan();
        mScanState = ScanState.STATE_SCANNING;
    }

    @Override
    public void stopScan() {
        stopLeScan();
        destroy();
        mScanCallback = null;
        mScanState = ScanState.STATE_IDLE;
    }

    private void delayScheduleStopScan() {
        removeHandlerMsg();
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScan();
                }
            }, mScanDuration);
        }
    }

    @Override
    protected void notifyScanStarted() {
        delayScheduleStopScan();
        if (mScannerPresenter != null) {
            mScannerPresenter.notifyScanStarted();
        }
    }

    @Override
    protected void notifyScanStopped() {
        if (mScannerPresenter != null) {
            mScannerPresenter.notifyScanStopped();
        }
    }
}
