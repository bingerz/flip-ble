package cn.bingerz.flipble.scanner.lescanner;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.bingerz.flipble.scanner.ScanFilterConfig;
import cn.bingerz.flipble.scanner.ScanRuleConfig;
import cn.bingerz.flipble.utils.EasyLog;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class LeScannerForJellyBeanMr2 extends LeScanner {

    private BluetoothAdapter.LeScanCallback leScanCallback;

    public LeScannerForJellyBeanMr2(BluetoothAdapter bluetoothAdapter, ScanRuleConfig config, LeScanCallback callback) {
        super(bluetoothAdapter, config, callback);
    }

    @Override
    protected void startScan() {
        postStartLeScan(parseServiceUUIDs(mScanRuleConfig));
    }

    @Override
    protected void stopScan() {
        postStopLeScan();
    }

    private List<ScanFilterConfig> parseScanFilterConfig(ScanRuleConfig config) {
        return config == null ? null : config.getScanFilterConfigs();
    }

    private UUID[] parseServiceUUIDs(ScanRuleConfig config) {
        if (config != null) {
            List<String> uuids = new ArrayList<>();
            List<ScanFilterConfig> scanFilterConfigs = config.getScanFilterConfigs();
            if (scanFilterConfigs != null && !scanFilterConfigs.isEmpty()) {
                for (ScanFilterConfig scanFilterConfig : scanFilterConfigs) {
                    if (!TextUtils.isEmpty(scanFilterConfig.getServiceUUID())) {
                        uuids.add(scanFilterConfig.getServiceUUID());
                    }
                }
            }
            if (!uuids.isEmpty()) {
                UUID[] serviceUUIDs = new UUID[uuids.size()];
                for (int i = 0; i < uuids.size(); i++) {
                    serviceUUIDs[i] = UUID.fromString(uuids.get(i));
                }
                return serviceUUIDs;
            }
        }
        return null;
    }

    private void postStartLeScan(final UUID[] serviceUUIds) {
        if (!isBluetoothOn()) {
            EasyLog.d("Not starting scan because bluetooth is off");
            return;
        }
        final BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            return;
        }
        final BluetoothAdapter.LeScanCallback leScanCallback = getLeScanCallback();
        mScanHandler.removeCallbacksAndMessages(null);
        mScanHandler.post(new Runnable() {
            @SuppressLint("MissingPermission")
            @WorkerThread
            @Override
            public void run() {
                try {
                    //noinspection deprecation
                    bluetoothAdapter.startLeScan(serviceUUIds, leScanCallback);
                } catch (Exception e) {
                    EasyLog.e(e, "Internal Android exception in startLeScan()");
                }
            }
        });
    }

    private void postStopLeScan() {
        if (!isBluetoothOn()) {
            EasyLog.d("Not stopping scan because bluetooth is off");
            return;
        }
        final BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            return;
        }
        final BluetoothAdapter.LeScanCallback leScanCallback = getLeScanCallback();
        mScanHandler.removeCallbacksAndMessages(null);
        mScanHandler.post(new Runnable() {
            @SuppressLint("MissingPermission")
            @WorkerThread
            @Override
            public void run() {
                try {
                    //noinspection deprecation
                    bluetoothAdapter.stopLeScan(leScanCallback);
                } catch (Exception e) {
                    EasyLog.e(e, "Internal Android exception in stopLeScan()");
                }
            }
        });
    }

    @SuppressWarnings({"MissingPermission"})
    private synchronized boolean isNeedDevice(BluetoothDevice device) {
        if (device == null) {
            EasyLog.e("device is null, device needs to be filtered.");
            return false;
        }
        List<ScanFilterConfig> filterConfigs = parseScanFilterConfig(mScanRuleConfig);
        if (filterConfigs == null || filterConfigs.isEmpty()) {
            return true;
        }

        String remoteName = device.getName() == null ? "" : device.getName();
        String remoteAddress = device.getAddress() == null ? "" : device.getAddress();

        AtomicBoolean equal = new AtomicBoolean(false);
        for (ScanFilterConfig filterConfig : filterConfigs) {
            String filterMac = filterConfig.getDeviceMac();
            String filterName = filterConfig.getDeviceName();
            //假设设备MAC地址始终不为空
            if (!TextUtils.isEmpty(filterMac) && remoteAddress.equals(filterMac)) {
                if (!TextUtils.isEmpty(filterName)) {
                    equal.set(remoteName.equals(filterName));
                } else {
                    equal.set(true);
                }
            } else {
                equal.set(true);
            }
        }
        return equal.get();
    }

    private BluetoothAdapter.LeScanCallback getLeScanCallback() {
        if (leScanCallback == null) {
            leScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    if (isNeedDevice(device)) {
                        mLeScanCallback.onLeScan(device, rssi, scanRecord);
                    }
                }
            };
        }
        return leScanCallback;
    }
}
