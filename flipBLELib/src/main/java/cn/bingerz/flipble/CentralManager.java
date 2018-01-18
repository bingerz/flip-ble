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
import cn.bingerz.flipble.callback.ConnectionStateCallback;
import cn.bingerz.flipble.callback.IndicateCallback;
import cn.bingerz.flipble.callback.MtuChangedCallback;
import cn.bingerz.flipble.callback.NotifyCallback;
import cn.bingerz.flipble.callback.ReadCallback;
import cn.bingerz.flipble.callback.RssiCallback;
import cn.bingerz.flipble.callback.WriteCallback;
import cn.bingerz.flipble.callback.ScanCallback;
import cn.bingerz.flipble.exception.BleException;
import cn.bingerz.flipble.exception.NotFoundDeviceException;
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
    private static final int DEFAULT_MTU = 23;
    private static final int DEFAULT_MAX_MTU = 512;

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
     *
     * @return
     */
    public CentralScanner getBleScanner() {
        return centralScanner;
    }

    /**
     * get the ScanRuleConfig
     *
     * @return
     */
    public BleScanRuleConfig getScanRuleConfig() {
        return bleScanRuleConfig;
    }

    /**
     * Configure scan and connection properties
     *
     * @param scanRuleConfig
     */
    public void initScanRule(BleScanRuleConfig scanRuleConfig) {
        this.bleScanRuleConfig = scanRuleConfig;
    }

    /**
     * scan device around
     *
     * @param callback
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
     *
     * @return
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
     *
     * @return
     */
    public MultiplePeripheralController getMultiplePeripheralController() {
        return mMultiPeripheralController;
    }

    /**
     * Get the maximum number of connections
     *
     * @return
     */
    public int getMaxConnectCount() {
        return maxConnectCount;
    }

    /**
     * Set the maximum number of connections
     *
     * @param maxCount
     * @return BleManager
     */
    public CentralManager setMaxConnectCount(int maxCount) {
        if (maxCount > DEFAULT_MAX_MULTIPLE_DEVICE)
            maxCount = DEFAULT_MAX_MULTIPLE_DEVICE;
        this.maxConnectCount = maxCount;
        return this;
    }

    /**
     * Get operate timeout
     *
     * @return
     */
    public int getOperateTimeout() {
        return operateTimeout;
    }

    /**
     * Set operate timeout
     *
     * @param operateTimeout
     * @return BleManager
     */
    public CentralManager setOperateTimeout(int operateTimeout) {
        this.operateTimeout = operateTimeout;
        return this;
    }


    /**
     * print log?
     *
     * @param enable
     * @return BleManager
     */
    public CentralManager enableLog(boolean enable) {
        BleLog.isPrint = enable;
        return this;
    }

    /**
     * connect a known device
     *
     * @param peripheral
     * @param connectionStateCallback
     * @return
     */
    public boolean connect(Peripheral peripheral, ConnectionStateCallback connectionStateCallback) {
        if (connectionStateCallback == null) {
            throw new IllegalArgumentException("BleGattCallback can not be Null!");
        }

        if (!isBlueEnable()) {
            handleException(new OtherException("BlueTooth not enable!"));
            return false;
        }

        if (peripheral == null) {
            connectionStateCallback.onConnectFail(new NotFoundDeviceException());
        } else {
            return peripheral.connect(false, connectionStateCallback);
        }
        return false;
    }

    /**
     * notify
     *
     * @param peripheral
     * @param uuid_service
     * @param uuid_notify
     * @param callback
     */
    public void notify(Peripheral peripheral, String uuid_service, String uuid_notify, NotifyCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleNotifyCallback can not be Null!");
        }

        if (peripheral == null) {
            callback.onNotifyFailure(new OtherException("This device not connect!"));
        } else {
            peripheral.newPeripheralController()
                    .withUUIDString(uuid_service, uuid_notify)
                    .enableCharacteristicNotify(callback, uuid_notify);
        }
    }

    /**
     * indicate
     *
     * @param peripheral
     * @param uuid_service
     * @param uuid_indicate
     * @param callback
     */
    public void indicate(Peripheral peripheral, String uuid_service, String uuid_indicate, IndicateCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleIndicateCallback can not be Null!");
        }

        if (peripheral == null) {
            callback.onIndicateFailure(new OtherException("This device not connect!"));
        } else {
            peripheral.newPeripheralController()
                    .withUUIDString(uuid_service, uuid_indicate)
                    .enableCharacteristicIndicate(callback, uuid_indicate);
        }
    }

    /**
     * stop notify, remove callback
     *
     * @param peripheral
     * @param serviceUUID
     * @param notifyUUID
     * @return
     */
    public boolean stopNotify(Peripheral peripheral, String serviceUUID, String notifyUUID) {
        if (peripheral == null) {
            return false;
        }
        boolean success = peripheral.newPeripheralController()
                .withUUIDString(serviceUUID, notifyUUID).disableCharacteristicNotify();
        if (success) {
            peripheral.removeNotifyCallback(notifyUUID);
        }
        return success;
    }

    /**
     * stop indicate, remove callback
     *
     * @param peripheral
     * @param serviceUUID
     * @param indicateUUID
     * @return
     */
    public boolean stopIndicate(Peripheral peripheral, String serviceUUID, String indicateUUID) {
        if (peripheral == null) {
            return false;
        }
        boolean success = peripheral.newPeripheralController()
                .withUUIDString(serviceUUID, indicateUUID).disableCharacteristicIndicate();
        if (success) {
            peripheral.removeIndicateCallback(indicateUUID);
        }
        return success;
    }

    /**
     * write
     *
     * @param peripheral
     * @param serviceUUID
     * @param writeUUID
     * @param data
     * @param callback
     */
    public void write(Peripheral peripheral, String serviceUUID, String writeUUID, byte[] data, WriteCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleWriteCallback can not be Null!");
        }

        if (data == null) {
            BleLog.e("data is Null!");
            callback.onWriteFailure(new OtherException("data is Null !"));
            return;
        }

        if (data.length > 20) {
            BleLog.w("data's length beyond 20!");
        }

        if (peripheral == null) {
            callback.onWriteFailure(new OtherException("This device not connect!"));
        } else {
            peripheral.newPeripheralController()
                    .withUUIDString(serviceUUID, writeUUID).writeCharacteristic(data, callback, writeUUID);
        }
    }

    /**
     * read
     *
     * @param peripheral
     * @param serviceUUID
     * @param readUUID
     * @param callback
     */
    public void read(Peripheral peripheral, String serviceUUID, String readUUID, ReadCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleReadCallback can not be Null!");
        }

        if (peripheral == null) {
            callback.onReadFailure(new OtherException("This device not connect!"));
        } else {
            peripheral.newPeripheralController()
                    .withUUIDString(serviceUUID, readUUID).readCharacteristic(callback, readUUID);
        }
    }

    /**
     * read Rssi
     *
     * @param peripheral
     * @param callback
     */
    public void readRssi(Peripheral peripheral, RssiCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleRssiCallback can not be Null!");
        }

        if (peripheral == null) {
            callback.onRssiFailure(new OtherException("This device not connect!"));
        } else {
            peripheral.newPeripheralController().readRemoteRssi(callback);
        }
    }

    /**
     * set Mtu
     *
     * @param peripheral
     * @param mtu
     * @param callback
     */
    public void setMtu(Peripheral peripheral, int mtu, MtuChangedCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleMtuChangedCallback can not be Null!");
        }

        if (mtu > DEFAULT_MAX_MTU) {
            BleLog.e("requiredMtu should lower than 512 !");
            callback.onSetMTUFailure(new OtherException("requiredMtu should lower than 512 !"));
            return;
        }

        if (mtu < DEFAULT_MTU) {
            BleLog.e("requiredMtu should higher than 23 !");
            callback.onSetMTUFailure(new OtherException("requiredMtu should higher than 23 !"));
            return;
        }

        if (peripheral == null) {
            callback.onSetMTUFailure(new OtherException("This device not connect!"));
        } else {
            peripheral.newPeripheralController().setMtu(mtu, callback);
        }
    }

    /**
     * is support ble?
     *
     * @return
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
     *
     * @return
     */
    public boolean isBlueEnable() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    public boolean isConnected(String key) {
        if (mMultiPeripheralController != null) {
            return mMultiPeripheralController.isContainDevice(key);
        }
        return false;
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
