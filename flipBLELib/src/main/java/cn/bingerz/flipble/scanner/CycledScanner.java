package cn.bingerz.flipble.scanner;

import android.annotation.TargetApi;
import android.os.Build;

import java.util.List;

import cn.bingerz.flipble.scanner.callback.ScanCallback;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CycledScanner extends Scanner {

    private CycledScannerPresenter mScannerPresenter;

    @Override
    public void initConfig(ScanRuleConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ScanRuleConfig is null.");
        }
        mScannerPresenter = new CycledScanner.myScannerPresenter(config);
        initLeScanner(config, mScannerPresenter);
    }

    @Override
    public void startScan(final ScanCallback callback) {
        mScanCallback = callback;
        startLeScan();
    }

    @Override
    public void stopScan() {
        if (mScannerPresenter != null) {
            mScannerPresenter.removeHandlerMsg();
        }
        stopLeScan();
        mScanCallback = null;
    }

    @Override
    protected void notifyScanStarted() {
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

    @Override
    public void destroy() {
        if (mScannerPresenter != null) {
            mScannerPresenter.removeHandlerMsg();
        }
        super.destroy();
    }

    private class myScannerPresenter extends CycledScannerPresenter {

        public myScannerPresenter(ScanRuleConfig config) {
            super(CycledScanner.this, config);
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
