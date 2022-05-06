package cn.bingerz.flipble.central;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import java.util.List;

import cn.bingerz.flipble.peripheral.ConnectionState;
import cn.bingerz.flipble.peripheral.MultiplePeripheralController;
import cn.bingerz.flipble.peripheral.Peripheral;
import cn.bingerz.flipble.exception.BLEException;
import cn.bingerz.flipble.exception.hanlder.DefaultExceptionHandler;
import cn.bingerz.flipble.scanner.ScanRuleConfig;
import cn.bingerz.flipble.scanner.Scanner;
import cn.bingerz.flipble.scanner.callback.ScanCallback;
import cn.bingerz.flipble.utils.BLEConnectionCompat;
import cn.bingerz.flipble.utils.EasyLog;
import cn.bingerz.flipble.utils.GeneralUtil;

/**
 * @author hanson
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
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private MultiplePeripheralController mMultiPeripheralController;

    private DefaultExceptionHandler mBLEExceptionHandler;

    private BLEConnectionCompat mConnectionCompat;

    private CentralManager() {
    }

    public static CentralManager getInstance() {
        return CentralManagerHolder.INSTANCE;
    }

    private static class CentralManagerHolder {
        private static final CentralManager INSTANCE = new CentralManager();
    }

    public void init(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Init exception, application is null.");
        }
        if (mContext == null) {
            mContext = context;
            EasyLog.setExplicitTag("FlipBLE");
            mBLEExceptionHandler = new DefaultExceptionHandler();
            mMultiPeripheralController = new MultiplePeripheralController();
            mConnectionCompat = new BLEConnectionCompat(context);
        }
    }

    public boolean isScanning() {
        return mScanner != null && mScanner.isScanning();
    }

    public void startScan(boolean isCycled, ScanRuleConfig config, ScanCallback callback) {
        stopScan();
        if (checkLocationPermission()) {
            mScanner = Scanner.createScanner(isCycled);
            mScanner.initConfig(config);
            mScanner.startScanner(callback);
        } else {
            EasyLog.e("StartScan is fail. Need to grant location permissions");
        }
    }

    public void stopScan() {
        if (mScanner != null) {
            mScanner.stopScanner();
        }
        mScanner = null;
    }

    public Context getContext() {
        return mContext;
    }

    private boolean checkLocationPermission() {
        return !GeneralUtil.isGreaterOrEqual6_0()
                || GeneralUtil.isGranted(mContext, Manifest.permission.ACCESS_FINE_LOCATION)
                || GeneralUtil.isGranted(mContext, Manifest.permission.ACCESS_COARSE_LOCATION);
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
            EasyLog.e(e, "Cannot construct bluetooth adapter.");
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

    public BLEConnectionCompat getConnectionCompat() {
        if (mConnectionCompat == null) {
            synchronized(CentralManager.class) {
                if (mConnectionCompat == null) {
                    mConnectionCompat = new BLEConnectionCompat(getContext());
                }
            }
        }
        return mConnectionCompat;
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
        if (maxCount > DEFAULT_MAX_MULTIPLE_DEVICE) {
            maxCount = DEFAULT_MAX_MULTIPLE_DEVICE;
        }
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
        return GeneralUtil.isSupportBle(mContext);
    }

    /**
     * Open bluetooth
     */
    public void enableBluetooth() {
        if (!isBluetoothGranted()) {
            EasyLog.e("Need BLUETOOTH_CONNECT permission.");
            return;
        }
        if (getBluetoothAdapter() != null) {
            getBluetoothAdapter().enable();
        }
    }

    /**
     * Disable bluetooth
     */
    public void disableBluetooth() {
        if (!isBluetoothGranted()) {
            EasyLog.e("Need BLUETOOTH_CONNECT permission.");
            return;
        }
        if (isBluetoothEnable()) {
            getBluetoothAdapter().disable();
        }
    }

    /**
     * Check Bluetooth enable status
     */
    public boolean isBluetoothEnable() {
        boolean result = false;
        try {
            result = getBluetoothAdapter() != null && getBluetoothAdapter().isEnabled();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Check Bluetooth grant status
     */
    public boolean isBluetoothGranted() {
        if (mContext == null) {
            throw new IllegalStateException("Context is not initialized.");
        }
        return GeneralUtil.isBluetoothGranted(mContext);
    }

    private BluetoothDevice retrieveDevice(String address) {
        return getBluetoothAdapter() != null ? getBluetoothAdapter().getRemoteDevice(address) : null;
    }

    private BluetoothDevice retrieveDevice(byte[] address) {
        return getBluetoothAdapter() != null ? getBluetoothAdapter().getRemoteDevice(address) : null;
    }

    public Peripheral retrievePeripheral(String address) {
        if (!TextUtils.isEmpty(address)) {
            BluetoothDevice device = retrieveDevice(address);
            return new Peripheral(device);
        }
        return null;
    }

    public Peripheral retrievePeripheral(byte[] address) {
        int macAddressLength = 6;
        if (!(address == null || address.length != macAddressLength)) {
            BluetoothDevice device = retrieveDevice(address);
            return new Peripheral(device);
        }
        return null;
    }

    public List<Peripheral> getAllConnectedDevice() {
        return mMultiPeripheralController != null ? mMultiPeripheralController.getPeripheralList() : null;
    }

    public boolean isBLEConnected(String address) {
        if (!BluetoothAdapter.checkBluetoothAddress(address)) {
            throw new IllegalArgumentException(address + " is not a valid Bluetooth address");
        } else if (!isBluetoothEnable()) {
            EasyLog.e("BluetoothAdapter is turn off.");
            return false;
        } else if (!isBluetoothGranted()) {
            EasyLog.e("Need BLUETOOTH_CONNECT permission.");
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

    public boolean isConnecting(String address) {
        Peripheral p = getPeripheral(address);
        return p != null && p.getConnectState() == ConnectionState.CONNECT_CONNECTING;
    }

    public boolean isConnected(String address) {
        Peripheral p = getPeripheral(address);
        return p != null && p.getConnectState() == ConnectionState.CONNECT_CONNECTED;
    }

    public Peripheral getPeripheral(String address) {
        return mMultiPeripheralController != null ? mMultiPeripheralController.getPeripheral(address) : null;
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
