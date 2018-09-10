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

import cn.bingerz.easylog.EasyLog;
import cn.bingerz.flipble.scanner.ScanFilterConfig;
import cn.bingerz.flipble.scanner.ScanRuleConfig;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LeScannerForLollipop extends LeScanner {

    private BluetoothLeScanner mScanner;
    private ScanCallback mScanCallback;

    public LeScannerForLollipop(BluetoothAdapter bluetoothAdapter, ScanRuleConfig config, LeScanCallback callback) {
        super(bluetoothAdapter, config, callback);
    }

    @Override
    protected void startScan() {
        ScanSettings settings = parseSettings(mScanRuleConfig);
        if (settings != null) {
            postStartLeScan(parseFilters(mScanRuleConfig), settings);
        }
    }

    @Override
    protected void stopScan() {
        postStopLeScan();
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

    private void postStartLeScan(final List<ScanFilter> filters, final ScanSettings settings) {
        if (!isBluetoothOn()) {
            EasyLog.d("Not starting scan because bluetooth is off");
            return;
        }
        final BluetoothLeScanner scanner = getScanner();
        if (scanner == null) {
            return;
        }
        final ScanCallback scanCallback = getNewLeScanCallback();
        mScanHandler.removeCallbacksAndMessages(null);
        mScanHandler.post(new Runnable() {
            @SuppressLint("MissingPermission")
            @WorkerThread
            @Override
            public void run() {
                try {
                    scanner.startScan(filters, settings, scanCallback);
                } catch (IllegalStateException e) {
                    EasyLog.w("Cannot start scan. Bluetooth may be turned off.");
                } catch (NullPointerException npe) {
                    // Necessary because of https://code.google.com/p/android/issues/detail?id=160503
                    EasyLog.e(npe, "Cannot start scan. Unexpected NPE.");
                } catch (SecurityException e) {
                    // Thrown by Samsung Knox devices if bluetooth access denied for an app
                    EasyLog.e("Cannot start scan.  Security Exception");
                }

            }
        });
    }

    private void postStopLeScan() {
        if (!isBluetoothOn()) {
            EasyLog.d("Not stopping scan because bluetooth is off");
            return;
        }
        final BluetoothLeScanner scanner = getScanner();
        if (scanner == null) {
            return;
        }
        final ScanCallback scanCallback = getNewLeScanCallback();
        mScanHandler.removeCallbacksAndMessages(null);
        mScanHandler.post(new Runnable() {
            @SuppressLint("MissingPermission")
            @WorkerThread
            @Override
            public void run() {
                try {
                    EasyLog.d("Stopping LE scan on scan handler");
                    scanner.stopScan(scanCallback);
                } catch (IllegalStateException e) {
                    EasyLog.w("Cannot stop scan. Bluetooth may be turned off.");
                } catch (NullPointerException npe) {
                    // Necessary because of https://code.google.com/p/android/issues/detail?id=160503
                    EasyLog.e(npe, "Cannot stop scan. Unexpected NPE.");
                } catch (SecurityException e) {
                    // Thrown by Samsung Knox devices if bluetooth access denied for an app
                    EasyLog.e("Cannot stop scan.  Security Exception");
                }

            }
        });
    }

    private BluetoothLeScanner getScanner() {
        try {
            if (mScanner == null) {
                EasyLog.d("Making new Android L scanner");
                BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
                if (bluetoothAdapter != null) {
                    mScanner = getBluetoothAdapter().getBluetoothLeScanner();
                }
                if (mScanner == null) {
                    EasyLog.w("Failed to make new Android L scanner");
                }
            }
        } catch (SecurityException e) {
            EasyLog.w("SecurityException making new Android L scanner");
        }
        return mScanner;
    }

    private ScanCallback getNewLeScanCallback() {
        if (mScanCallback == null) {
            mScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult scanResult) {
                    mLeScanCallback.onLeScan(scanResult.getDevice(),
                            scanResult.getRssi(), scanResult.getScanRecord().getBytes());
                }

                @Override
                public void onBatchScanResults(List<ScanResult> results) {
                    for (ScanResult scanResult : results) {
                        EasyLog.e("scanned device %s  rssi %d",
                                scanResult.getDevice().getAddress(), scanResult.getRssi());
                        mLeScanCallback.onLeScan(scanResult.getDevice(),
                                scanResult.getRssi(), scanResult.getScanRecord().getBytes());
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    switch (errorCode) {
                        case SCAN_FAILED_ALREADY_STARTED:
                            EasyLog.e("Scan failed: a BLE scan with the same settings is already started by the app");
                            break;
                        case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED:
                            EasyLog.e("Scan failed: app cannot be registered");
                            break;
                        case SCAN_FAILED_FEATURE_UNSUPPORTED:
                            EasyLog.e("Scan failed: power optimized scan feature is not supported");
                            break;
                        case SCAN_FAILED_INTERNAL_ERROR:
                            EasyLog.e("Scan failed: internal error");
                            break;
                        default:
                            EasyLog.e("Scan failed with unknown error (errorCode=" + errorCode + ")");
                            break;
                    }
                }
            };
        }
        return mScanCallback;
    }
}
