package cn.bingerz.flipble.scanner;

import android.annotation.TargetApi;
import android.os.Build;

import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.scanner.callback.ScanCallback;
import cn.bingerz.flipble.utils.EasyLog;

/**
 * @author hanson
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
    public void startScanner(final ScanCallback callback) {
        mScanCallback = callback;
        startLeScanner();
    }

    @Override
    public void stopScanner() {
        stopLeScanner();
        destroy();
        mScanCallback = null;
    }

    private void delayScheduleStopScan() {
        removeHandlerMsg();
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    EasyLog.v("OnceScanner schedule stopScan run");
                    stopScanner();
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
