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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.central.ScanRecord;
import cn.bingerz.flipble.peripheral.callback.ConnectionStateCallback;
import cn.bingerz.flipble.peripheral.callback.IndicateCallback;
import cn.bingerz.flipble.peripheral.callback.MtuChangedCallback;
import cn.bingerz.flipble.peripheral.callback.NotifyCallback;
import cn.bingerz.flipble.peripheral.callback.ReadCallback;
import cn.bingerz.flipble.peripheral.callback.RssiCallback;
import cn.bingerz.flipble.peripheral.callback.WriteCallback;
import cn.bingerz.flipble.exception.ConnectionException;
import cn.bingerz.flipble.exception.GattException;
import cn.bingerz.flipble.exception.OtherException;
import cn.bingerz.flipble.utils.EasyLog;

/**
 * Created by hanson on 09/01/2018.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Peripheral {

    private static final int DEFAULT_MTU = 23;
    private static final int DEFAULT_MAX_MTU = 512;

    private ConnectionState connectState = ConnectionState.CONNECT_IDLE;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isActivityDisconnect = false;

    private int mRssi;
    private ScanRecord mScanRecord;
    private BluetoothDevice mDevice;
    private BluetoothGatt mBluetoothGatt;

    private ConnectionStateCallback connectionStateCallback;
    private RssiCallback rssiCallback;
    private MtuChangedCallback mtuChangedCallback;
    private HashMap<String, NotifyCallback> notifyCallbackHashMap = new HashMap<>();
    private HashMap<String, IndicateCallback> indicateCallbackHashMap = new HashMap<>();
    private HashMap<String, WriteCallback> writeCallbackHashMap = new HashMap<>();
    private HashMap<String, ReadCallback> readCallbackHashMap = new HashMap<>();


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
        return mRssi;
    }

    public ScanRecord getScanRecord() {
        return mScanRecord;
    }

    public ConnectionState getConnectState() {
        return connectState;
    }

    public BluetoothGatt getBluetoothGatt() {
        return mBluetoothGatt;
    }

    public synchronized void addConnectionStateCallback(ConnectionStateCallback callback) {
        this.connectionStateCallback = callback;
    }

    public synchronized void removeConnectionStateCallback() {
        this.connectionStateCallback = null;
    }

    public synchronized void addNotifyCallback(String uuid, NotifyCallback notifyCallback) {
        notifyCallbackHashMap.put(uuid, notifyCallback);
    }

    public synchronized void addIndicateCallback(String uuid, IndicateCallback indicateCallback) {
        indicateCallbackHashMap.put(uuid, indicateCallback);
    }

    public synchronized void addWriteCallback(String uuid, WriteCallback writeCallback) {
        writeCallbackHashMap.put(uuid, writeCallback);
    }

    public synchronized void addReadCallback(String uuid, ReadCallback readCallback) {
        readCallbackHashMap.put(uuid, readCallback);
    }

    public synchronized void removeNotifyCallback(String uuid) {
        if (notifyCallbackHashMap.containsKey(uuid))
            notifyCallbackHashMap.remove(uuid);
    }

    public synchronized void removeIndicateCallback(String uuid) {
        if (indicateCallbackHashMap.containsKey(uuid))
            indicateCallbackHashMap.remove(uuid);
    }

    public synchronized void removeWriteCallback(String uuid) {
        if (writeCallbackHashMap.containsKey(uuid))
            writeCallbackHashMap.remove(uuid);
    }

    public synchronized void removeReadCallback(String uuid) {
        if (readCallbackHashMap.containsKey(uuid))
            readCallbackHashMap.remove(uuid);
    }

    public synchronized void clearCharacterCallback() {
        if (notifyCallbackHashMap != null)
            notifyCallbackHashMap.clear();
        if (indicateCallbackHashMap != null)
            indicateCallbackHashMap.clear();
        if (writeCallbackHashMap != null)
            writeCallbackHashMap.clear();
        if (readCallbackHashMap != null)
            readCallbackHashMap.clear();
    }

    public synchronized void addRssiCallback(RssiCallback callback) {
        rssiCallback = callback;
    }

    public synchronized void removeRssiCallback() {
        rssiCallback = null;
    }

    public synchronized void addMtuChangedCallback(MtuChangedCallback callback) {
        mtuChangedCallback = callback;
    }

    public synchronized void removeMtuChangedCallback() {
        mtuChangedCallback = null;
    }

    private boolean connect(boolean autoConnect, ConnectionStateCallback callback) {
        EasyLog.i("connect device:%s mac:%s autoConnect:%s", getName(), getAddress(), autoConnect);
        addConnectionStateCallback(callback);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = mDevice.connectGatt(CentralManager.getInstance().getContext(), autoConnect,
                    coreGattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            mBluetoothGatt = mDevice.connectGatt(CentralManager.getInstance().getContext(), autoConnect, coreGattCallback);
        }
        if (mBluetoothGatt != null) {
            if (connectionStateCallback != null) {
                connectionStateCallback.onStartConnect();
            }
            connectState = ConnectionState.CONNECT_CONNECTING;
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
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    private synchronized void closeBluetoothGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
    }

    public synchronized void destroy() {
        connectState = ConnectionState.CONNECT_IDLE;
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
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
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
    public synchronized boolean connect(ConnectionStateCallback connectionStateCallback) {
        if (connectionStateCallback == null) {
            throw new IllegalArgumentException("BleGattCallback can not be Null!");
        }

        if (!CentralManager.getInstance().isBluetoothEnable()) {
            CentralManager.getInstance().handleException(new OtherException("BlueTooth not enable!"));
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
        boolean success = newPeripheralController().withUUIDString(serviceUUID, indicateUUID)
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

        newPeripheralController().withUUIDString(serviceUUID, writeUUID).writeCharacteristic(data, callback, writeUUID);
    }

    /**
     * read
     */
    public void read(String serviceUUID, String readUUID, ReadCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleReadCallback can not be Null!");
        }

        newPeripheralController().withUUIDString(serviceUUID, readUUID)
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
            EasyLog.i("BluetoothGattCallback：Connection State Change\nstatus: %d\nnewState: %d\ncurrentThread: %d",
                    status, newState, Thread.currentThread().getId());

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();

            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                closeBluetoothGatt();
                CentralManager.getInstance().getMultiplePeripheralController().removePeripheral(Peripheral.this);
                if (connectState == ConnectionState.CONNECT_CONNECTING) {
                    connectState = ConnectionState.CONNECT_FAILURE;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (connectionStateCallback != null)
                                connectionStateCallback.onConnectFail(new ConnectionException(gatt, status));
                        }
                    });

                } else if (connectState == ConnectionState.CONNECT_CONNECTED) {
                    connectState = ConnectionState.CONNECT_DISCONNECT;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (connectionStateCallback != null)
                                connectionStateCallback.onDisConnected(isActivityDisconnect, Peripheral.this, newState);
                        }
                    });
                }
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            super.onServicesDiscovered(gatt, status);
            EasyLog.i("BluetoothGattCallback：services discovered\nstatus: %d\ncurrentThread: %d",
                    status, Thread.currentThread().getId());

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBluetoothGatt = gatt;
                connectState = ConnectionState.CONNECT_CONNECTED;
                isActivityDisconnect = false;
                CentralManager.getInstance().getMultiplePeripheralController().addPeripheral(Peripheral.this);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (connectionStateCallback != null)
                            connectionStateCallback.onConnectSuccess(Peripheral.this, status);
                    }
                });
            } else {
                closeBluetoothGatt();
                connectState = ConnectionState.CONNECT_FAILURE;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (connectionStateCallback != null)
                            connectionStateCallback.onConnectFail(new ConnectionException(gatt, status));
                    }
                });
            }
        }

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            EasyLog.i("BluetoothGattCallback：onCharacteristicChanged ");

            Iterator iterator = notifyCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object call = entry.getValue();
                if (call instanceof NotifyCallback) {
                    if (characteristic.getUuid().toString().equalsIgnoreCase(((NotifyCallback) call).getKey())) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                ((NotifyCallback) call).onCharacteristicChanged(characteristic.getValue());
                            }
                        });
                    }
                }
            }

            iterator = indicateCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object call = entry.getValue();
                if (call instanceof IndicateCallback) {
                    if (characteristic.getUuid().toString().equalsIgnoreCase(((IndicateCallback) call).getKey())) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                ((IndicateCallback) call).onCharacteristicChanged(characteristic.getValue());
                            }
                        });
                    }
                }
            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            EasyLog.i("BluetoothGattCallback：onCharacteristicWrite ");

            Iterator iterator = writeCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object call = entry.getValue();
                if (call instanceof WriteCallback) {
                    if (characteristic.getUuid().toString().equalsIgnoreCase(((WriteCallback) call).getKey())) {
                        ((WriteCallback) call).getPeripheralConnector().writeMsgInit();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    ((WriteCallback) call).onWriteSuccess();
                                } else {
                                    ((WriteCallback) call).onWriteFailure(new GattException(status));
                                }
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            EasyLog.i("BluetoothGattCallback：onCharacteristicRead ");

            Iterator iterator = readCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object call = entry.getValue();
                if (call instanceof ReadCallback) {
                    if (characteristic.getUuid().toString().equalsIgnoreCase(((ReadCallback) call).getKey())) {
                        ((ReadCallback) call).getPeripheralConnector().readMsgInit();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    ((ReadCallback) call).onReadSuccess(characteristic.getValue());
                                } else {
                                    ((ReadCallback) call).onReadFailure(new GattException(status));
                                }
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, final int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            EasyLog.i("GattCallback：onDescriptorWrite ");

            Iterator iterator = notifyCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object call = entry.getValue();
                if (call instanceof NotifyCallback) {
                    if (descriptor.getCharacteristic().getUuid().toString().equalsIgnoreCase(((NotifyCallback) call).getKey())) {
                        ((NotifyCallback) call).getPeripheralConnector().notifyMsgInit();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    ((NotifyCallback) call).onNotifySuccess();
                                } else {
                                    ((NotifyCallback) call).onNotifyFailure(new GattException(status));
                                }
                            }
                        });
                    }
                }
            }

            iterator = indicateCallbackHashMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                final Object call = entry.getValue();
                if (call instanceof IndicateCallback) {
                    if (descriptor.getCharacteristic().getUuid().toString().equalsIgnoreCase(((IndicateCallback) call).getKey())) {
                        ((IndicateCallback) call).getPeripheralConnector().indicateMsgInit();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (status == BluetoothGatt.GATT_SUCCESS) {
                                    ((IndicateCallback) call).onIndicateSuccess();
                                } else {
                                    ((IndicateCallback) call).onIndicateFailure(new GattException(status));
                                }
                            }
                        });
                    }
                }
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            EasyLog.i("GattCallback：onDescriptorRead ");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, final int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            EasyLog.i("BluetoothGattCallback：onReadRemoteRssi status: %d", status);

            if (rssiCallback != null) {
                rssiCallback.getPeripheralConnector().rssiMsgInit();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            rssiCallback.onRssiSuccess(rssi);
                        } else {
                            rssiCallback.onRssiFailure(new GattException(status));
                        }
                    }
                });
            }

        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, final int mtu, final int status) {
            super.onMtuChanged(gatt, mtu, status);
            EasyLog.i("BluetoothGattCallback：onMtuChanged ");

            if (mtuChangedCallback != null) {
                mtuChangedCallback.getPeripheralConnector().mtuChangedMsgInit();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            mtuChangedCallback.onMtuChanged(mtu);
                        } else {
                            mtuChangedCallback.onSetMTUFailure(new GattException(status));
                        }
                    }
                });
            }

        }
    };
}
