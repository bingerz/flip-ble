package cn.bingerz.flipble;

import android.annotation.TargetApi;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.List;
import java.util.UUID;

import cn.bingerz.flipble.bluetoothle.MultiplePeripheralController;
import cn.bingerz.flipble.bluetoothle.Peripheral;
import cn.bingerz.flipble.callback.ScanCallback;
import cn.bingerz.flipble.exception.BleException;
import cn.bingerz.flipble.exception.OtherException;
import cn.bingerz.flipble.exception.hanlder.DefaultBleExceptionHandler;
import cn.bingerz.flipble.scan.BleScanRuleConfig;
import cn.bingerz.flipble.scan.CentralScanner;
import cn.bingerz.flipble.utils.BleLog;

/**
 * Created by hanson on 09/01/2018.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CentralManager {

    public static final int DEFAULT_SCAN_TIME = 10000;
    private static final int DEFAULT_MAX_MULTIPLE_DEVICE = 7;
    private static final int DEFAULT_OPERATE_TIME = 5000;

    private int operateTimeout = DEFAULT_OPERATE_TIME;
    private int maxConnectCount = DEFAULT_MAX_MULTIPLE_DEVICE;

    private CentralScanner centralScanner;
    private BleScanRuleConfig bleScanRuleConfig;

    private Application mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private MultiplePeripheralController mMultiPeripheralController;
    private DefaultBleExceptionHandler bleExceptionHandler;

    private CentralManager() {}

    public static CentralManager getInstance() {
        return CentralManagerHolder.sCentralManager;
    }

    private static class CentralManagerHolder {
        private static final CentralManager sCentralManager = new CentralManager();
    }

    public void init(Application application) {
        if (mContext == null && application != null) {
            mContext = application;
            BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                mBluetoothAdapter = bluetoothManager.getAdapter();
            }
            mMultiPeripheralController = new MultiplePeripheralController();

            bleScanRuleConfig = new BleScanRuleConfig();
            centralScanner = CentralScanner.getInstance();
        }
    }

    /**
     * Get the BleScanner
     */
    public CentralScanner getBleScanner() {
        return centralScanner;
    }

    /**
     * get the ScanRuleConfig
     */
    public BleScanRuleConfig getScanRuleConfig() {
        return bleScanRuleConfig;
    }

    /**
     * Configure scan and connection properties
     */
    public void initScanRule(BleScanRuleConfig scanRuleConfig) {
        this.bleScanRuleConfig = scanRuleConfig;
    }

    /**
     * scan device around
     */
    public void scan(ScanCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleScanCallback can not be Null!");
        }

        if (!isBlueEnable()) {
            handleException(new OtherException("BlueTooth not enable!"));
            return;
        }

        UUID[] serviceUuids = bleScanRuleConfig.getServiceUuids();
        String[] deviceNames = bleScanRuleConfig.getDeviceNames();
        String deviceMac = bleScanRuleConfig.getDeviceMac();
        boolean fuzzy = bleScanRuleConfig.isFuzzy();
        long timeOut = bleScanRuleConfig.getScanTimeOut();

        centralScanner.scan(serviceUuids, callback);
    }

    /**
     * Cancel scan
     */
    public void cancelScan() {
        centralScanner.stopLeScan();
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * Get the BluetoothAdapter
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }


    /**
     * Handle Exception Information
     */
    public void handleException(BleException exception) {
        bleExceptionHandler.handleException(exception);
    }

    /**
     * Get the multiple peripheral Controller
     */
    public MultiplePeripheralController getMultiplePeripheralController() {
        return mMultiPeripheralController;
    }

    /**
     * Get the maximum number of connections
     */
    public int getMaxConnectCount() {
        return maxConnectCount;
    }

    /**
     * Set the maximum number of connections
     */
    public CentralManager setMaxConnectCount(int maxCount) {
        if (maxCount > DEFAULT_MAX_MULTIPLE_DEVICE)
            maxCount = DEFAULT_MAX_MULTIPLE_DEVICE;
        this.maxConnectCount = maxCount;
        return this;
    }

    /**
     * Get operate timeout
     */
    public int getOperateTimeout() {
        return operateTimeout;
    }

    /**
     * Set operate timeout
     */
    public CentralManager setOperateTimeout(int operateTimeout) {
        this.operateTimeout = operateTimeout;
        return this;
    }


    /**
     * print log?
     */
    public CentralManager enableLog(boolean enable) {
        BleLog.isPrint = enable;
        return this;
    }


    /**
     * is support ble?
     */
    public boolean isSupportBle() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && mContext.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Open bluetooth
     */
    public void enableBluetooth() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.enable();
        }
    }

    /**
     * Disable bluetooth
     */
    public void disableBluetooth() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled())
                mBluetoothAdapter.disable();
        }
    }

    public List<Peripheral> getAllConnectedDevice() {
        if (mMultiPeripheralController == null)
            return null;
        return mMultiPeripheralController.getPeripheralList();
    }

    /**
     * judge Bluetooth is enable
     */
    public boolean isBlueEnable() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    public boolean isConnected(String key) {
        return mMultiPeripheralController != null && mMultiPeripheralController.isContainDevice(key);
    }

    public Peripheral getPeripheral(String key) {
        if (mMultiPeripheralController != null) {
            return mMultiPeripheralController.getPeripheral(key);
        }
        return null;
    }

    public void disconnectAllDevice() {
        if (mMultiPeripheralController != null) {
            mMultiPeripheralController.disconnectAllDevice();
        }
    }

    public void destroy() {
        if (mMultiPeripheralController != null) {
            mMultiPeripheralController.destroy();
        }
    }
}
