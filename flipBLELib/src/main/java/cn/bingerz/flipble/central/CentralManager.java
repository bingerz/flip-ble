package cn.bingerz.flipble.central;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import java.util.List;

import cn.bingerz.flipble.peripheral.MultiplePeripheralController;
import cn.bingerz.flipble.peripheral.Peripheral;
import cn.bingerz.flipble.exception.BLEException;
import cn.bingerz.flipble.exception.hanlder.DefaultExceptionHandler;
import cn.bingerz.flipble.scanner.ScanRuleConfig;
import cn.bingerz.flipble.scanner.Scanner;
import cn.bingerz.flipble.scanner.callback.ScanCallback;
import cn.bingerz.flipble.utils.EasyLog;

/**
 * Created by hanson on 09/01/2018.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CentralManager {

    /**
     * The default duration in milliseconds of the Bluetooth scan duration when clients are in the foreground
     */
    public static final int DEFAULT_FOREGROUND_SCAN_DURATION = 6000;
    /**
     * The default duration in milliseconds of the Bluetooth scan interval when clients are in the foreground
     */
    public static final int DEFAULT_FOREGROUND_SCAN_INTERVAL = 6000;
    /**
     * The default duration in milliseconds of the Bluetooth scan duration when clients are in the background
     */
    public static final int DEFAULT_BACKGROUND_SCAN_DURATION = 10000;
    /**
     * The default duration in milliseconds of the Bluetooth scan interval when clients are in the background
     */
    public static final int DEFAULT_BACKGROUND_SCAN_INTERVAL = 5 * 60 * 1000;

    private static final int DEFAULT_MAX_MULTIPLE_DEVICE = 7;
    private static final int DEFAULT_OPERATE_TIME = 5000;

    private int operateTimeout = DEFAULT_OPERATE_TIME;
    private int maxConnectCount = DEFAULT_MAX_MULTIPLE_DEVICE;

    private Scanner mScanner;
    private Application mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private MultiplePeripheralController mMultiPeripheralController;

    private DefaultExceptionHandler mBLEExceptionHandler;

    private CentralManager() {
    }

    public static CentralManager getInstance() {
        return CentralManagerHolder.INSTANCE;
    }

    private static class CentralManagerHolder {
        private static final CentralManager INSTANCE = new CentralManager();
    }

    public void init(Application application) {
        if (application == null) {
            throw new IllegalArgumentException("Init exception, application is null.");
        }
        if (mContext == null) {
            mContext = application;
            EasyLog.setExplicitTag("FlipBLE");
            mBLEExceptionHandler = new DefaultExceptionHandler();
            mMultiPeripheralController = new MultiplePeripheralController();
        }
    }

    public boolean isScanning() {
        return mScanner != null && mScanner.isScanning();
    }

    public void startScan(boolean isCycled, ScanRuleConfig config, ScanCallback callback) {
        stopScan();
        if (android.os.Build.VERSION.SDK_INT < 23 || checkLocationPermission()) {
            mScanner = Scanner.createScanner(isCycled);
            mScanner.initConfig(config);
            mScanner.startScan(callback);
        } else {
            EasyLog.e("startScan is failure. Need to grant location permissions");
        }
    }

    public void stopScan() {
        if (mScanner != null && mScanner.isScanning()) {
            mScanner.stopScan();
        }
        mScanner = null;
    }

    public Context getContext() {
        return mContext;
    }

    private boolean checkLocationPermission() {
        return checkPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                || checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
    }

    private boolean checkPermission(final String permission) {
        return mContext.checkPermission(permission, android.os.Process.myPid(), android.os.Process.myUid()) == PackageManager.PERMISSION_GRANTED;
    }

    private BluetoothManager getBluetoothManager() {
        return mContext == null ? null :
                (BluetoothManager) mContext.getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
    }

    /**
     * Get the BluetoothAdapter
     */
    public BluetoothAdapter getBluetoothAdapter() {
        try {
            if (mBluetoothAdapter == null) {
                final BluetoothManager bluetoothManager = getBluetoothManager();
                if (bluetoothManager != null) {
                    mBluetoothAdapter = bluetoothManager.getAdapter();
                    if (mBluetoothAdapter == null) {
                        EasyLog.w("Failed to construct a BluetoothAdapter");
                    }
                }
            }
        } catch (SecurityException e) {
            // Thrown by Samsung Knox devices if bluetooth access denied for an app
            EasyLog.e("Cannot construct bluetooth adapter. Security Exception");
        }
        return mBluetoothAdapter;
    }


    /**
     * Handle Exception Information
     */
    public void handleException(BLEException exception) {
        mBLEExceptionHandler.handleException(exception);
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
        EasyLog.setLoggable(enable);
        return this;
    }

    /**
     * is support ble?
     */
    public boolean isSupportBle() {
        if (mContext == null) {
            throw new IllegalStateException("Context is not initialized.");
        }
        PackageManager packageManager = mContext.getApplicationContext().getPackageManager();
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Open bluetooth
     */
    @SuppressWarnings({"MissingPermission"})
    public void enableBluetooth() {
        if (getBluetoothAdapter() != null) {
            getBluetoothAdapter().enable();
        }
    }

    /**
     * Disable bluetooth
     */
    @SuppressWarnings({"MissingPermission"})
    public void disableBluetooth() {
        if (isBluetoothEnable()) {
            getBluetoothAdapter().disable();
        }
    }

    /**
     * judge Bluetooth is enable
     */
    @SuppressWarnings({"MissingPermission"})
    public boolean isBluetoothEnable() {
        boolean result = false;
        try {
            result = getBluetoothAdapter() != null && getBluetoothAdapter().isEnabled();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private BluetoothDevice retrieveDevice(String address) {
        if (getBluetoothAdapter() != null) {
            return getBluetoothAdapter().getRemoteDevice(address);
        }
        return null;
    }

    private BluetoothDevice retrieveDevice(byte[] address) {
        if (getBluetoothAdapter() != null) {
            return getBluetoothAdapter().getRemoteDevice(address);
        }
        return null;
    }

    public Peripheral retrievePeripheral(String address) {
        if (!TextUtils.isEmpty(address)) {
            BluetoothDevice device = retrieveDevice(address);
            return new Peripheral(device);
        }
        return null;
    }

    public Peripheral retrievePeripheral(byte[] address) {
        if (!(address == null || address.length != 6)) {
            BluetoothDevice device = retrieveDevice(address);
            return new Peripheral(device);
        }
        return null;
    }

    public List<Peripheral> getAllConnectedDevice() {
        if (mMultiPeripheralController == null)
            return null;
        return mMultiPeripheralController.getPeripheralList();
    }

    @SuppressWarnings({"MissingPermission"})
    public boolean isBLEConnected(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            throw new IllegalArgumentException(address + " is not a valid Bluetooth address");
        } else if (!isBluetoothEnable()) {
            EasyLog.e("BT adapter is not turn on.");
            return false;
        } else {
            BluetoothDevice device = retrieveDevice(address);
            BluetoothManager bluetoothManager = getBluetoothManager();
            int state = BluetoothProfile.STATE_DISCONNECTED;
            if (bluetoothManager != null) {
                state = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
            }
            return state == BluetoothProfile.STATE_CONNECTED;
        }
    }

    public boolean isConnected(String address) {
        return mMultiPeripheralController != null && mMultiPeripheralController.isContainDevice(address);
    }

    public Peripheral getPeripheral(String address) {
        if (mMultiPeripheralController == null)
            return null;
        return mMultiPeripheralController.getPeripheral(address);
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
