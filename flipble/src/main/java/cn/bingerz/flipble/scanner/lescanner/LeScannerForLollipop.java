package cn.bingerz.flipble.scanner.lescanner;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import cn.bingerz.flipble.scanner.ScanFilterConfig;
import cn.bingerz.flipble.scanner.ScanRuleConfig;
import cn.bingerz.flipble.utils.EasyLog;

/**
 * @author hanson
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LeScannerForLollipop extends LeScanner {

    private BluetoothLeScanner mScanner;
    private ScanCallback mScanCallback;

    public LeScannerForLollipop(BluetoothAdapter bluetoothAdapter, ScanRuleConfig config, LeScanCallback callback) {
        super(bluetoothAdapter, config, callback);
    }

    @Override
    protected void startLeScanner() {
        postStartLeScan();
    }

    @Override
    protected void stopLeScanner() {
        //TODO Hanson postStopLeScan();
        stopLeScanHandler();
    }

    private void postStartLeScan() {
        postToWorkerThread(true, new Runnable() {
            @WorkerThread
            @Override
            public void run() {
                startLeScanHandler();
            }
        });
    }

    private void postStopLeScan() {
        postToWorkerThread(true, new Runnable() {
            @WorkerThread
            @Override
            public void run() {
                stopLeScanHandler();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void startLeScanHandler() {
        final BluetoothLeScanner scanner = getScanner();
        final List<ScanFilter> filters = parseFilters(mScanRuleConfig);
        final ScanSettings settings = parseSettings(mScanRuleConfig);
        if (scanner == null || settings == null) {
            EasyLog.e("StartLeScanHandler fail, scanner or settings is null.");
            return;
        }
        final ScanCallback scanCallback = getNewLeScanCallback();
        try {
            EasyLog.v("Starting LE scan on scan handler");
            scanner.startScan(filters, settings, scanCallback);
            setScanState(LeScanState.STATE_SCANNED);
        } catch (IllegalStateException e) {
            EasyLog.e(e, "Cannot start scan. Bluetooth may be turned off.");
        } catch (NullPointerException e) {
            // Necessary because of https://code.google.com/p/android/issues/detail?id=160503
            EasyLog.e(e, "Cannot start scan. Unexpected NPE.");
        } catch (SecurityException e) {
            // Thrown by Samsung Knox devices if bluetooth access denied for an app
            EasyLog.e(e, "Cannot start scan. Security Exception");
        }
    }

    @SuppressLint("MissingPermission")
    private void stopLeScanHandler() {
        final BluetoothLeScanner scanner = getScanner();
        if (scanner == null) {
            EasyLog.e("StopLeScanHandler fail, scanner is null");
            return;
        }
        final ScanCallback scanCallback = getNewLeScanCallback();
        try {
            if (isLeScanned()) {
                EasyLog.v("Stopping LE scan on scan handler");
                scanner.stopScan(scanCallback);
                setScanState(LeScanState.STATE_IDLE);
            } else {
                EasyLog.w("LeScanner been Stopped");
            }
        } catch (IllegalStateException e) {
            EasyLog.e(e, "Cannot stop scan. Bluetooth may be turned off.");
        } catch (NullPointerException e) {
            // Necessary because of https://code.google.com/p/android/issues/detail?id=160503
            EasyLog.e(e, "Cannot stop scan. Unexpected NPE.");
        } catch (SecurityException e) {
            // Thrown by Samsung Knox devices if bluetooth access denied for an app
            EasyLog.e(e, "Cannot stop scan. Security Exception");
        }
    }

    private BluetoothLeScanner getScanner() {
        try {
            if (mScanner == null) {
                EasyLog.v("Making new Android L scanner");
                BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
                if (bluetoothAdapter != null) {
                    mScanner = getBluetoothAdapter().getBluetoothLeScanner();
                }
                if (mScanner == null) {
                    EasyLog.w("Failed to make new Android L scanner");
                }
            }
        } catch (SecurityException e) {
            EasyLog.e(e, "SecurityException making new Android L scanner");
        }
        return mScanner;
    }

    private ScanCallback getNewLeScanCallback() {
        if (mScanCallback == null) {
            mScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult scanResult) {
                    if (mLeScanCallback != null) {
                        mLeScanCallback.onLeScan(scanResult.getDevice(),
                                scanResult.getRssi(), scanResult.getScanRecord().getBytes());
                    }
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    for (ScanResult scanResult : results) {
                        EasyLog.d("Scanned device=%s  rssi=%d",
                                    scanResult.getDevice().getAddress(), scanResult.getRssi());
                        if (mLeScanCallback != null) {
                            mLeScanCallback.onLeScan(scanResult.getDevice(),
                                    scanResult.getRssi(), scanResult.getScanRecord().getBytes());
                        }
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    setScanState(LeScanState.STATE_IDLE);
                    switch (errorCode) {
                        case SCAN_FAILED_ALREADY_STARTED:
                            EasyLog.e("Scan failed, A BLE scan with the same settings is already started by the app");
                            break;
                        case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                            EasyLog.e("Scan failed, App cannot be registered");
                            break;
                        case SCAN_FAILED_FEATURE_UNSUPPORTED:
                            EasyLog.e("Scan failed, Power optimized scan feature is not supported");
                            break;
                        case SCAN_FAILED_INTERNAL_ERROR:
                            EasyLog.e("Scan failed with internal error");
                            break;
                        default:
                            EasyLog.e("Scan failed with unknown error (errorCode=%d)", errorCode);
                            break;
                    }
                    if (mLeScanCallback != null) {
                        mLeScanCallback.onLeScanFailed(errorCode);
                    }
                }
            };
        }
        return mScanCallback;
    }

    private List<ScanFilter> parseFilters(ScanRuleConfig config) {
        List<ScanFilter> filters = new ArrayList<>();
        if (config != null) {
            List<ScanFilterConfig> scanFilterConfigs = config.getScanFilterConfigs();
            if (scanFilterConfigs != null && !scanFilterConfigs.isEmpty()) {
                for (ScanFilterConfig scanFilterConfig : scanFilterConfigs) {
                    ScanFilter.Builder builder = new ScanFilter.Builder();
                    if (!TextUtils.isEmpty(scanFilterConfig.getDeviceMac())) {
                        builder.setDeviceAddress(scanFilterConfig.getDeviceMac());
                    }
                    if (!TextUtils.isEmpty(scanFilterConfig.getDeviceName())) {
                        builder.setDeviceName(scanFilterConfig.getDeviceName());
                    }
                    if (!TextUtils.isEmpty(scanFilterConfig.getServiceUUID())) {
                        ParcelUuid parcelUuid = ParcelUuid.fromString(scanFilterConfig.getServiceUUID());
                        ParcelUuid parcelUuidMask = ParcelUuid.fromString("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF");
                        builder.setServiceUuid(parcelUuid, parcelUuidMask);
                    }
                    filters.add(builder.build());
                }
            }
        }
        return filters;
    }

    private ScanSettings parseSettings(ScanRuleConfig config) {
        ScanSettings settings = null;
        if (config != null) {
            ScanSettings.Builder builder = new ScanSettings.Builder();
            switch (config.getScanMode()) {
                case ScanRuleConfig.SCAN_MODE_LOW_POWER:
                    builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
                    break;

                case ScanRuleConfig.SCAN_MODE_BALANCED:
                    builder.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
                    break;

                case ScanRuleConfig.SCAN_MODE_HIGH_POWER:
                    builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
                    break;

                default:
                    builder.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY);
                    break;
            }
            settings = builder.build();
        }
        return settings;
    }
}
