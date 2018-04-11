package cn.bingerz.flipble.peripheral;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.central.ScanRecord;
import cn.bingerz.flipble.exception.ConnectException;
import cn.bingerz.flipble.peripheral.callback.ConnectStateCallback;
import cn.bingerz.flipble.peripheral.callback.IndicateCallback;
import cn.bingerz.flipble.peripheral.callback.MtuChangedCallback;
import cn.bingerz.flipble.peripheral.callback.NotifyCallback;
import cn.bingerz.flipble.peripheral.callback.ReadCallback;
import cn.bingerz.flipble.peripheral.callback.RssiCallback;
import cn.bingerz.flipble.peripheral.callback.WriteCallback;
import cn.bingerz.flipble.exception.GattException;
import cn.bingerz.flipble.exception.OtherException;
import cn.bingerz.flipble.utils.EasyLog;
import cn.bingerz.flipble.utils.HexUtil;

/**
 * Created by hanson on 09/01/2018.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Peripheral {

    private static final int DEFAULT_MTU = 23;
    private static final int DEFAULT_MAX_MTU = 512;

    private ConnectionState mConnectState = ConnectionState.CONNECT_IDLE;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean isActivityDisconnect = false;

    private float mRssi;
    private float mCov;//卡尔曼滤波用的协方差估计值(Covariance estimation)

    private ScanRecord mScanRecord;
    private BluetoothDevice mDevice;
    private BluetoothGatt mBluetoothGatt;

    private ConnectStateCallback mConnectStateCallback;
    private RssiCallback mRssiCallback;
    private MtuChangedCallback mMtuChangedCallback;
    private Map<String, NotifyCallback> mNotifyCallbackMap = new ConcurrentHashMap<>();
    private Map<String, IndicateCallback> mIndicateCallbackMap = new ConcurrentHashMap<>();
    private Map<String, WriteCallback> mWriteCallbackMap = new ConcurrentHashMap<>();
    private Map<String, ReadCallback> mReadCallbackMap = new ConcurrentHashMap<>();

    public Peripheral(BluetoothDevice device) {
        this.mDevice = device;
    }

    public Peripheral(BluetoothDevice device, int rssi, ScanRecord scanRecord) {
        this.mRssi = rssi;
        this.mDevice = device;
        this.mScanRecord = scanRecord;
    }

    public PeripheralController newPeripheralController() {
        return new PeripheralController(this);
    }

    public String getName() {
        if (mDevice != null) {
            return mDevice.getName();
        }
        return null;
    }

    public String getAddress() {
        if (mDevice != null) {
            return mDevice.getAddress();
        }
        return null;
    }

    public String getKey() {
        return getName() + getAddress();
    }

    public int getRssi() {
        return (int) this.mRssi;
    }

    /**
     * Calculate and predict real rssi values based on new values, affected by last rssi value.
     */
    public int getFliterRssi(int newRSSI) {
        int R = 1, Q = 1, A = 1, B = 0, C = 1;
        int u = 0;
        if (this.mRssi == 0) {
            this.mRssi = (1 / C) * newRSSI;
            this.mCov = (1 / C) * Q * (1 / C);
        } else {
            final float predX = (A * this.mRssi) + (B * u);
            final float predCov = ((A * this.mCov) * A) + R;

            final float K = predCov * C * (1 / ((C * predCov * C) + Q));

            this.mRssi = predX + K * (newRSSI - (C * predX));
            this.mCov = predCov - (K * C * predCov);
        }
        return getRssi();
    }

    public ScanRecord getScanRecord() {
        return mScanRecord;
    }

    public ConnectionState getConnectState() {
        return mConnectState;
    }

    public BluetoothGatt getBluetoothGatt() {
        return mBluetoothGatt;
    }

    public synchronized void addConnectionStateCallback(ConnectStateCallback callback) {
        this.mConnectStateCallback = callback;
    }

    public synchronized void removeConnectionStateCallback() {
        this.mConnectStateCallback = null;
    }

    public synchronized void addNotifyCallback(String uuid, NotifyCallback notifyCallback) {
        mNotifyCallbackMap.put(uuid, notifyCallback);
    }

    public synchronized void removeNotifyCallback(String uuid) {
        if (mNotifyCallbackMap.containsKey(uuid)) {
            mNotifyCallbackMap.remove(uuid);
        }
    }

    private NotifyCallback findNotifyCallback(String uuid) {
        if (TextUtils.isEmpty(uuid)) {
            return null;
        }
        Iterator iterator = mNotifyCallbackMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            NotifyCallback notifyCallback = (NotifyCallback) entry.getValue();
            if (notifyCallback.getKey().equalsIgnoreCase(uuid)) {
                return notifyCallback;
            }
        }
        return null;
    }

    public synchronized void addIndicateCallback(String uuid, IndicateCallback indicateCallback) {
        mIndicateCallbackMap.put(uuid, indicateCallback);
    }

    public synchronized void removeIndicateCallback(String uuid) {
        if (mIndicateCallbackMap.containsKey(uuid)) {
            mIndicateCallbackMap.remove(uuid);
        }
    }

    private IndicateCallback findIndicateCallback(String uuid) {
        if (TextUtils.isEmpty(uuid)) {
            return null;
        }
        Iterator iterator = mIndicateCallbackMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            IndicateCallback indicateCallback = (IndicateCallback) entry.getValue();
            if (indicateCallback.getKey().equalsIgnoreCase(uuid)) {
                return indicateCallback;
            }
        }
        return null;
    }

    public synchronized void addWriteCallback(String uuid, WriteCallback writeCallback) {
        mWriteCallbackMap.put(uuid, writeCallback);
    }

    public synchronized void removeWriteCallback(String uuid) {
        if (mWriteCallbackMap.containsKey(uuid)) {
            mWriteCallbackMap.remove(uuid);
        }
    }

    private WriteCallback findWriteCallback(String uuid) {
        if (TextUtils.isEmpty(uuid)) {
            return null;
        }
        Iterator iterator = mWriteCallbackMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            WriteCallback writeCallback = (WriteCallback) entry.getValue();
            if (writeCallback.getKey().equalsIgnoreCase(uuid)) {
                return writeCallback;
            }
        }
        return null;
    }

    public synchronized void addReadCallback(String uuid, ReadCallback readCallback) {
        mReadCallbackMap.put(uuid, readCallback);
    }

    public synchronized void removeReadCallback(String uuid) {
        if (mReadCallbackMap.containsKey(uuid)) {
            mReadCallbackMap.remove(uuid);
        }
    }

    private ReadCallback findReadCallback(String uuid) {
        if (TextUtils.isEmpty(uuid)) {
            return null;
        }
        Iterator iterator = mReadCallbackMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry entry = (Map.Entry) iterator.next();
            ReadCallback readCallback = (ReadCallback) entry.getValue();
            if (readCallback.getKey().equalsIgnoreCase(uuid)) {
                return readCallback;
            }
        }
        return null;
    }

    public synchronized void clearCharacterCallback() {
        if (mNotifyCallbackMap != null) {
            mNotifyCallbackMap.clear();
        }
        if (mIndicateCallbackMap != null) {
            mIndicateCallbackMap.clear();
        }
        if (mWriteCallbackMap != null) {
            mWriteCallbackMap.clear();
        }
        if (mReadCallbackMap != null) {
            mReadCallbackMap.clear();
        }
    }

    public synchronized void addRssiCallback(RssiCallback callback) {
        mRssiCallback = callback;
    }

    public synchronized void removeRssiCallback() {
        mRssiCallback = null;
    }

    public synchronized void addMtuChangedCallback(MtuChangedCallback callback) {
        mMtuChangedCallback = callback;
    }

    public synchronized void removeMtuChangedCallback() {
        mMtuChangedCallback = null;
    }

    private boolean connect(boolean autoConnect, ConnectStateCallback callback) {
        EasyLog.i("connect device:%s mac:%s autoConnect:%s", getName(), getAddress(), autoConnect);
        addConnectionStateCallback(callback);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = mDevice.connectGatt(CentralManager.getInstance().getContext(), autoConnect,
                    coreGattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            mBluetoothGatt = mDevice.connectGatt(CentralManager.getInstance().getContext(), autoConnect, coreGattCallback);
        }
        if (mBluetoothGatt != null) {
            if (mConnectStateCallback != null) {
                mConnectStateCallback.onStartConnect();
            }
            mConnectState = ConnectionState.CONNECT_CONNECTING;
            return true;
        }
        return false;
    }

    public synchronized boolean refreshDeviceCache() {
        try {
            final Method refresh = BluetoothGatt.class.getMethod("refresh");
            if (refresh != null) {
                boolean success = (Boolean) refresh.invoke(getBluetoothGatt());
                EasyLog.i("refreshDeviceCache, is success: " + success);
                return success;
            }
        } catch (Exception e) {
            EasyLog.i("exception occur while refreshing device: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public synchronized void disconnect() {
        if (mBluetoothGatt != null) {
            isActivityDisconnect = true;
            mBluetoothGatt.disconnect();
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    private synchronized void closeBluetoothGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
    }

    public synchronized void destroy() {
        mConnectState = ConnectionState.CONNECT_IDLE;
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
        if (mBluetoothGatt != null) {
            refreshDeviceCache();
        }
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
        removeConnectionStateCallback();
        removeRssiCallback();
        removeMtuChangedCallback();
        clearCharacterCallback();
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    public boolean isContainService(String serviceUUID) {
        return getService(serviceUUID) != null;
    }

    private BluetoothGattService getService(String serviceUUID) {
        if (mBluetoothGatt == null) {
            return null;
        }
        return mBluetoothGatt.getService(UUID.fromString(serviceUUID));
    }

    public boolean isContainCharact(String serviceUUID, String charactUUID) {
        return getCharact(serviceUUID, charactUUID) != null;
    }

    private BluetoothGattCharacteristic getCharact(String serviceUUID, String charactUUID) {
        BluetoothGattService service = getService(serviceUUID);
        if (service == null) {
            return null;
        }
        return service.getCharacteristic(UUID.fromString(charactUUID));
    }

    /**
     * connect a known device
     */
    public synchronized boolean connect(ConnectStateCallback connectionStateCallback) {
        if (connectionStateCallback == null) {
            throw new IllegalArgumentException("BleGattCallback can not be Null!");
        }

        if (!CentralManager.getInstance().isBluetoothEnable()) {
            CentralManager.getInstance().handleException(new OtherException("BT adapter is not turn on."));
            return false;
        }

        return connect(false, connectionStateCallback);
    }

    /**
     * notify
     */
    public void notify(String serviceUUID, String notifyUUID, NotifyCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleNotifyCallback can not be Null!");
        }

        newPeripheralController().withUUIDString(serviceUUID, notifyUUID)
                .enableCharacteristicNotify(callback, notifyUUID);
    }

    /**
     * indicate
     */
    public void indicate(String serviceUUID, String indicateUUID, IndicateCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleIndicateCallback can not be Null!");
        }

        newPeripheralController().withUUIDString(serviceUUID, indicateUUID)
                .enableCharacteristicIndicate(callback, indicateUUID);
    }

    /**
     * stop notify, remove callback
     */
    public boolean stopNotify(String serviceUUID, String notifyUUID) {
        boolean success = newPeripheralController().withUUIDString(serviceUUID, notifyUUID)
                .disableCharacteristicNotify();
        if (success) {
            removeNotifyCallback(notifyUUID);
        }
        return success;
    }

    /**
     * stop indicate, remove callback
     */
    public boolean stopIndicate(String serviceUUID, String indicateUUID) {
        boolean success = newPeripheralController()
                .withUUIDString(serviceUUID, indicateUUID)
                .disableCharacteristicIndicate();
        if (success) {
            removeIndicateCallback(indicateUUID);
        }
        return success;
    }

    /**
     * write
     */
    public void write(String serviceUUID, String writeUUID, byte[] data, WriteCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleWriteCallback can not be Null!");
        }

        if (data == null) {
            EasyLog.e("data is Null!");
            callback.onWriteFailure(new OtherException("data is Null !"));
            return;
        }

        if (data.length > 20) {
            EasyLog.w("data's length beyond 20!");
        }

        newPeripheralController()
                .withUUIDString(serviceUUID, writeUUID)
                .writeCharacteristic(data, callback, writeUUID);
    }

    /**
     * read
     */
    public void read(String serviceUUID, String readUUID, ReadCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleReadCallback can not be Null!");
        }

        newPeripheralController()
                .withUUIDString(serviceUUID, readUUID)
                .readCharacteristic(callback, readUUID);
    }

    /**
     * read Rssi
     */
    public void readRssi(RssiCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleRssiCallback can not be Null!");
        }

        newPeripheralController().readRemoteRssi(callback);
    }

    /**
     * set Mtu
     */
    public void setMtu(int mtu, MtuChangedCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleMtuChangedCallback can not be Null!");
        }

        if (mtu > DEFAULT_MAX_MTU) {
            EasyLog.e("requiredMtu should lower than 512 !");
            callback.onSetMTUFailure(new OtherException("requiredMtu should lower than 512 !"));
            return;
        }

        if (mtu < DEFAULT_MTU) {
            EasyLog.e("requiredMtu should higher than 23 !");
            callback.onSetMTUFailure(new OtherException("requiredMtu should higher than 23 !"));
            return;
        }

        newPeripheralController().setMtu(mtu, callback);
    }

    private BluetoothGattCallback coreGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            EasyLog.i("BluetoothGattCallback：Connection State Change\nstatus: %d  newState: %d  currentThread: %d",
                    status, newState, Thread.currentThread().getId());

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                closeBluetoothGatt();
                CentralManager.getInstance().getMultiplePeripheralController().removePeripheral(Peripheral.this);
                if (mConnectState == ConnectionState.CONNECT_CONNECTING) {
                    mConnectState = ConnectionState.CONNECT_FAILURE;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mConnectStateCallback != null) {
                                mConnectStateCallback.onConnectFail(new ConnectException(status));
                            }
                        }
                    });

                } else if (mConnectState == ConnectionState.CONNECT_CONNECTED) {
                    mConnectState = ConnectionState.CONNECT_DISCONNECT;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mConnectStateCallback != null) {
                                mConnectStateCallback.onDisConnected(isActivityDisconnect, Peripheral.this, status);
                            }
                        }
                    });
                }
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            super.onServicesDiscovered(gatt, status);
            EasyLog.i("BluetoothGattCallback：services discovered\nstatus: %d  currentThread: %d",
                    status, Thread.currentThread().getId());

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBluetoothGatt = gatt;
                mConnectState = ConnectionState.CONNECT_CONNECTED;
                isActivityDisconnect = false;
                CentralManager.getInstance().getMultiplePeripheralController().addPeripheral(Peripheral.this);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mConnectStateCallback != null) {
                            mConnectStateCallback.onConnectSuccess(Peripheral.this, status);
                        }
                    }
                });
            } else {
                closeBluetoothGatt();
                mConnectState = ConnectionState.CONNECT_FAILURE;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mConnectStateCallback != null) {
                            mConnectStateCallback.onConnectFail(new ConnectException(status));
                        }
                    }
                });
            }
        }

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            EasyLog.i("BluetoothGattCallback：onCharacteristicChanged ");
            EasyLog.d("uuid：%s value: %s", characteristic.getUuid(), HexUtil.encodeHexStr(characteristic.getValue()));

            final NotifyCallback notifyCallback = findNotifyCallback(characteristic.getUuid().toString());
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (notifyCallback != null) {
                        notifyCallback.onCharacteristicChanged(characteristic.getValue());
                    }
                }
            });

            final IndicateCallback indicateCallback = findIndicateCallback(characteristic.getUuid().toString());
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (indicateCallback != null) {
                        indicateCallback.onCharacteristicChanged(characteristic.getValue());
                    }
                }
            });
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            EasyLog.i("BluetoothGattCallback：onCharacteristicWrite ");
            EasyLog.d("uuid：%s value: %s", characteristic.getUuid(), HexUtil.encodeHexStr(characteristic.getValue()));

            final WriteCallback writeCallback = findWriteCallback(characteristic.getUuid().toString());
            if (writeCallback != null) {
                writeCallback.getPeripheralConnector().writeMsgInit();
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (writeCallback != null) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            writeCallback.onWriteSuccess();
                        } else {
                            writeCallback.onWriteFailure(new GattException(status));
                        }
                    }
                }
            });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            EasyLog.i("BluetoothGattCallback：onCharacteristicRead ");
            EasyLog.d("uuid：%s value: %s", characteristic.getUuid(), HexUtil.encodeHexStr(characteristic.getValue()));

            final ReadCallback readCallback = findReadCallback(characteristic.getUuid().toString());
            if (readCallback != null) {
                readCallback.getPeripheralConnector().readMsgInit();
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (readCallback != null) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            readCallback.onReadSuccess(characteristic.getValue());
                        } else {
                            readCallback.onReadFailure(new GattException(status));
                        }
                    }
                }
            });
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, final int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            EasyLog.i("GattCallback：onDescriptorWrite ");
            EasyLog.d("uuid：%s value: %s", descriptor.getUuid(), HexUtil.encodeHexStr(descriptor.getValue()));

            String uuid = descriptor.getCharacteristic().getUuid().toString();

            final NotifyCallback notifyCallback = findNotifyCallback(uuid);
            if (notifyCallback != null) {
                notifyCallback.getPeripheralConnector().notifyMsgInit();
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (notifyCallback != null) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            notifyCallback.onNotifySuccess();
                        } else {
                            notifyCallback.onNotifyFailure(new GattException(status));
                        }
                    }
                }
            });

            final IndicateCallback indicateCallback = findIndicateCallback(uuid);
            if (indicateCallback != null) {
                indicateCallback.getPeripheralConnector().notifyMsgInit();
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (indicateCallback != null) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            indicateCallback.onIndicateSuccess();
                        } else {
                            indicateCallback.onIndicateFailure(new GattException(status));
                        }
                    }
                }
            });
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            EasyLog.i("GattCallback：onDescriptorRead ");
            EasyLog.d("uuid：%s value: %s", descriptor.getUuid(), HexUtil.encodeHexStr(descriptor.getValue()));
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, final int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            EasyLog.i("BluetoothGattCallback：onReadRemoteRssi status: %d", status);

            if (mRssiCallback != null) {
                mRssiCallback.getPeripheralConnector().rssiMsgInit();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            mRssiCallback.onRssiSuccess(rssi);
                        } else {
                            mRssiCallback.onRssiFailure(new GattException(status));
                        }
                    }
                });
            }

        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, final int mtu, final int status) {
            super.onMtuChanged(gatt, mtu, status);
            EasyLog.i("BluetoothGattCallback：onMtuChanged ");

            if (mMtuChangedCallback != null) {
                mMtuChangedCallback.getPeripheralConnector().mtuChangedMsgInit();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            mMtuChangedCallback.onMtuChanged(mtu);
                        } else {
                            mMtuChangedCallback.onSetMTUFailure(new GattException(status));
                        }
                    }
                });
            }

        }
    };
}
