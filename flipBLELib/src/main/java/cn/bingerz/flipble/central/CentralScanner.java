package cn.bingerz.flipble.central;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.os.Build;

import java.util.List;
import java.util.UUID;

import cn.bingerz.flipble.exception.ScanException;
import cn.bingerz.flipble.peripheral.Peripheral;
import cn.bingerz.flipble.central.callback.ScanCallback;

/**
 * Created by hanson on 10/01/2018.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CentralScanner {

    public static CentralScanner getInstance() {
        return CentralScannerHolder.sCentralScanner;
    }

    private static class CentralScannerHolder {
        private static final CentralScanner sCentralScanner = new CentralScanner();
    }

    private CentralScannerPresenter centralScannerPresenter;
    private CentralScanState scanState = CentralScanState.STATE_IDLE;

    public void scan(UUID[] serviceUUIDs, String[] names, String mac, boolean fuzzy, long timeOut,
                     final ScanCallback callback) {
        startLeScan(serviceUUIDs, new CentralScannerPresenter(names, mac, fuzzy, timeOut) {
            @Override
            public void onScanStarted(boolean success) {
                if (callback != null) {
                    callback.onScanStarted(success);
                }
            }

            @Override
            public void onLeScan(Peripheral peripheral) {
                if (callback != null) {
                    callback.onLeScan(peripheral);
                }
            }

            @Override
            public void onScanning(Peripheral result) {
                if (callback != null) {
                    callback.onScanning(result);
                }
            }

            @Override
            public void onScanFinished(List<Peripheral> scanResultList) {
                if (callback != null) {
                    callback.onScanFinished(scanResultList);
                }
            }
        });
    }

    private synchronized void startLeScan(UUID[] serviceUUIDs, CentralScannerPresenter presenter) {
        if (presenter == null) {
            throw new IllegalArgumentException("CentralScannerPresenter is null.");
        }

        if (scanState == CentralScanState.STATE_SCANNING) {
            throw new IllegalStateException("Central Scanner is running.");
        }

        this.centralScannerPresenter = presenter;
        boolean success = false;
        BluetoothAdapter bluetoothAdapter = CentralManager.getInstance().getBluetoothAdapter();
        if (CentralManager.getInstance().isBluetoothEnable()) {
            success = bluetoothAdapter.startLeScan(serviceUUIDs, presenter);
        } else {
            CentralManager.getInstance().handleException(new ScanException("BT adapter is not turn on."));
        }
        scanState = success ? CentralScanState.STATE_SCANNING : CentralScanState.STATE_IDLE;
        centralScannerPresenter.notifyScanStarted(success);
    }

    public synchronized void stopLeScan() {
        if (centralScannerPresenter == null) {
            return;
        }

        if (scanState == CentralScanState.STATE_IDLE) {
            throw new IllegalStateException("Central Scanner is stopped.");
        }

        BluetoothAdapter bluetoothAdapter = CentralManager.getInstance().getBluetoothAdapter();
        if (CentralManager.getInstance().isBluetoothEnable()) {
            bluetoothAdapter.stopLeScan(centralScannerPresenter);
        } else {
            CentralManager.getInstance().handleException(new ScanException("BT adapter is not turn on."));
        }
        scanState = CentralScanState.STATE_IDLE;
        centralScannerPresenter.notifyScanStopped();
    }

    public CentralScanState getScanState() {
        return scanState;
    }

}
