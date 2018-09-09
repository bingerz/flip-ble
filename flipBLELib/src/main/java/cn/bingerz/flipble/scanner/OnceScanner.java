package cn.bingerz.flipble.scanner;

import android.annotation.TargetApi;
import android.os.Build;

import java.util.List;

import cn.bingerz.flipble.scanner.callback.ScanCallback;

/**
 * Created by hanson on 10/01/2018.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class OnceScanner extends Scanner {

    private OnceScannerPresenter mScannerPresenter;

    @Override
    public void initConfig(ScanRuleConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ScanRuleConfig is null.");
        }
        mScannerPresenter = new OnceScanner.myScannerPresenter(config);
        initLeScanner(config, mScannerPresenter);
    }

    @Override
    public void startScan(final ScanCallback callback) {
        mScanCallback = callback;
        startLeScan();
    }

    @Override
    public void stopScan() {
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

    private class myScannerPresenter extends OnceScannerPresenter {

        public myScannerPresenter(ScanRuleConfig config) {
            super(OnceScanner.this, config);
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
