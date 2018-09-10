package cn.bingerz.flipble.scanner;

import android.annotation.TargetApi;
import android.os.Build;

import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.scanner.callback.ScanCallback;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CycledScanner extends Scanner {

    private boolean mBackgroundMode;

    private long foregroundScanDuration = CentralManager.DEFAULT_FOREGROUND_SCAN_DURATION;
    private long foregroundScanInterval = CentralManager.DEFAULT_FOREGROUND_SCAN_INTERVAL;
    private long backgroundScanDuration = CentralManager.DEFAULT_BACKGROUND_SCAN_DURATION;
    private long backgroundScanInterval = CentralManager.DEFAULT_BACKGROUND_SCAN_INTERVAL;

    @Override
    public void initConfig(ScanRuleConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ScanRuleConfig is null.");
        }
        this.mBackgroundMode = config.isBackgroundMode();
        if (config.getScanDuration() > 0) {
            setDuration(mBackgroundMode, config.getScanDuration());
        }
        if (config.getScanInterval() > 0) {
            setInterval(mBackgroundMode, config.getScanInterval());
        }
        initLeScanner(config);
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
        mScanState = ScanState.STATE_SCANNING;
    }

    private void delayScheduleStartScan() {
        removeHandlerMsg();
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startLeScan();
                }
            }, getInterval());
        }
    }

    private void delayScheduleStopScan() {
        removeHandlerMsg();
        if (mHandler != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopLeScan();
                }
            }, getDuration());
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
        delayScheduleStartScan();
        if (mScannerPresenter != null) {
            mScannerPresenter.notifyScanStopped();
        }
    }
}
