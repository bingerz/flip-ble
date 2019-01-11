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
import android.os.Message;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import cn.bingerz.easylog.EasyLog;
import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.exception.ConnectException;
import cn.bingerz.flipble.exception.GattException;
import cn.bingerz.flipble.exception.OtherException;
import cn.bingerz.flipble.peripheral.callback.ConnectStateCallback;
import cn.bingerz.flipble.peripheral.callback.IndicateCallback;
import cn.bingerz.flipble.peripheral.callback.MtuChangedCallback;
import cn.bingerz.flipble.peripheral.callback.NotifyCallback;
import cn.bingerz.flipble.peripheral.callback.ReadCallback;
import cn.bingerz.flipble.peripheral.callback.RssiCallback;
import cn.bingerz.flipble.peripheral.callback.WriteCallback;
import cn.bingerz.flipble.scanner.ScanDevice;
import cn.bingerz.flipble.utils.BLEConnectionCompat;
import cn.bingerz.flipble.utils.HexUtil;

/**
 * Created by hanson on 09/01/2018.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Peripheral {

    private static final int MSG_CONNECT_FAILURE = 0x11;
    private static final int MSG_CONNECT_SUCCESS = 0x12;
    private static final int MSG_DISCOVER_SERVICE = 0x13;
    private static final int MSG_WRITE_CALLBACK = 0x14;
    private static final int MSG_NOTIFY_CALLBACK = 0x15;
    private static final int MSG_INDICATE_CALLBACK = 0x16;
    private static final int MSG_RSSI_CALLBACK = 0x17;
    private static final int MSG_MTU_CHANGE = 0x18;

    private static final int DEFAULT_MTU = 23;
    private static final int DEFAULT_MAX_MTU = 512;
    private static final int DEFAULT_DELAY_DISCOVER_SERVICE = 600;

    private static final int DEFAULT_CONNECT_RETRY_COUNT = 2;
    private static final int DEFAULT_DELAY_RETRY_CONNECT = 500;

    private ConnectionState mConnectState = ConnectionState.CONNECT_IDLE;

    //Client actively performs the disconnect method
    private boolean isActivityDisconnect = false;

    private float mRssi;
    private float mCov; //卡尔曼滤波用的协方差估计值(Covariance estimation)

    private int mConnectRetryCount;

    private ScanDevice mDevice;
    private BluetoothGatt mBluetoothGatt;

    private ConnectStateCallback mConnectStateCallback;
    private RssiCallback mRssiCallback;
    private MtuChangedCallback mMtuChangedCallback;
    private Map<String, NotifyCallback> mNotifyCallbackMap = new ConcurrentHashMap<>();
    private Map<String, IndicateCallback> mIndicateCallbackMap = new ConcurrentHashMap<>();
    private Map<String, WriteCallback> mWriteCallbackMap = new ConcurrentHashMap<>();
    private Map<String, ReadCallback> mReadCallbackMap = new ConcurrentHashMap<>();

    private Handler mMainHandler = new MyHandler(Looper.getMainLooper());

    private static final class MyHandler extends Handler {

        public MyHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CONNECT_FAILURE:
                    int status = msg.arg1;
                    ConnectStateCallback connectStateCallback = (ConnectStateCallback) msg.obj;
                    if (connectStateCallback != null) {
                        connectStateCallback.onConnectFail(new ConnectException(status));
                    }
                    break;
                case MSG_DISCOVER_SERVICE:
                    BluetoothGatt gatt = (BluetoothGatt) msg.obj;
                    if (gatt != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                        }
                        gatt.discoverServices();
                    }
                    msg.obj = null;
                    break;
                case MSG_WRITE_CALLBACK:
                    status = msg.arg1;
                    WriteCallback writeCallback = (WriteCallback) msg.obj;
                    if (writeCallback != null) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            writeCallback.onWriteSuccess();
                        } else {
                            writeCallback.onWriteFailure(new GattException(status));
                        }
                    }
                    msg.obj = null;
                    break;
                case MSG_NOTIFY_CALLBACK:
                    status = msg.arg1;
                    NotifyCallback notifyCallback = (NotifyCallback) msg.obj;
                    if (notifyCallback != null) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            notifyCallback.onNotifySuccess();
                        } else {
                            notifyCallback.onNotifyFailure(new GattException(status));
                        }
                    }
                    msg.obj = null;
                    break;
                case MSG_INDICATE_CALLBACK:
                    status = msg.arg1;
                    IndicateCallback indicateCallback = (IndicateCallback) msg.obj;
                    if (indicateCallback != null) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            indicateCallback.onIndicateSuccess();
                        } else {
                            indicateCallback.onIndicateFailure(new GattException(status));
                        }
                    }
                    msg.obj = null;
                    break;
                case MSG_RSSI_CALLBACK:
                    status = msg.arg1;
                    int rssi = msg.arg2;
                    RssiCallback rssiCallback = (RssiCallback) msg.obj;
                    if (rssiCallback != null) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            rssiCallback.onRssiSuccess(rssi);
                        } else {
                            rssiCallback.onRssiFailure(new GattException(status));
                        }
                    }
                    msg.obj = null;
                    break;
                case MSG_MTU_CHANGE:
                    status = msg.arg1;
                    int mtu = msg.arg2;
                    MtuChangedCallback mtuChangedCallback = (MtuChangedCallback) msg.obj;
                    if (mtuChangedCallback != null) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            mtuChangedCallback.onMtuChanged(mtu);
                        } else {
                            mtuChangedCallback.onSetMTUFailure(new GattException(status));
                        }
                    }
                    msg.obj = null;
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    public Peripheral(BluetoothDevice device) {
        this.mDevice = new ScanDevice(device, 0, null);
    }

    public Peripheral(ScanDevice device) {
        this.mDevice = device;
    }

    public PeripheralController newPeripheralController() {
        return new PeripheralController(this);
    }

    private Handler getMainHandler() {
        if (mMainHandler == null) {
            synchronized (Peripheral.class) {
                if (mMainHandler == null) {
                    mMainHandler = new MyHandler(Looper.getMainLooper());
                }
            }
        }
        return mMainHandler;
    }

    private void sendMsgToMainH(int what, int arg1, int arg2, Object obj) {
        getMainHandler().sendMessage(getMainHandler().obtainMessage(what, arg1, arg2, obj));
    }

    public ScanDevice getDevice() {
        return mDevice;
    }

    public BluetoothDevice getBluetoothDevice() {
        if (mDevice != null) {
            return mDevice.getBluetoothDevice();
        }
        return null;
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

    /**
     * connect a known device
     */
    public synchronized boolean connect(boolean autoConnect, ConnectStateCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleGattCallback can not be Null!");
        } else if (!CentralManager.getInstance().isBluetoothEnable()) {
            CentralManager.getInstance().handleException(new OtherException("BT adapter is not turn on."));
            return false;
        } else {
            if (CentralManager.getInstance().isScanning()) {
                CentralManager.getInstance().handleException(
                        new OtherException("When connecting the device, Recommended to stop scanning"));
            }
            if (mConnectState == ConnectionState.CONNECT_CONNECTING) {
                EasyLog.w("After connect(true/false) to retry connect is dangerous");
                closeBluetoothGatt();
            }
            EasyLog.i("connect device:%s mac:%s autoConnect:%s", getName(), getAddress(), autoConnect);
            addConnectionStateCallback(callback);

            mBluetoothGatt = connectGatt(autoConnect);
            if (mBluetoothGatt != null) {
                mConnectState = ConnectionState.CONNECT_CONNECTING;
                mConnectRetryCount = DEFAULT_CONNECT_RETRY_COUNT;
                if (mConnectStateCallback != null) {
                    mConnectStateCallback.onStartConnect();
                }
                return true;
            } else {
                EasyLog.e("connect device fail, bluetoothGatt is null.");
            }
            return false;
        }
    }

    private BluetoothGatt connectGatt(boolean autoConnect) {
        BluetoothGatt bluetoothGatt = null;
        BluetoothDevice device = getBluetoothDevice();
        if (device == null) {
            EasyLog.i("connect device fail, bluetooth device is null.");
            return null;
        }
        BLEConnectionCompat connectionCompat = CentralManager.getInstance().getConnectionCompat();
        if (connectionCompat != null) {
            bluetoothGatt = connectionCompat.connectGatt(device, autoConnect, coreGattCallback);
        }
        return bluetoothGatt;
    }

    public synchronized void disconnect() {
        if (mBluetoothGatt != null) {
            isActivityDisconnect = true;
            mBluetoothGatt.disconnect();
        }
        getMainHandler().removeCallbacksAndMessages(null);
    }

    private synchronized void closeBluetoothGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
        }
    }

    public synchronized void destroy() {
        mConnectState = ConnectionState.CONNECT_IDLE;
        //Add try catch code block, Binder(IPC) NullPointerException, Parcel.readException
        try {
            mBluetoothGatt.disconnect();
            refreshDeviceCache();
            mBluetoothGatt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        removeConnectionStateCallback();
        removeRssiCallback();
        removeMtuChangedCallback();
        clearCharacterCallback();
        getMainHandler().removeCallbacksAndMessages(null);
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

    public boolean isContainProperty(String serviceUUID, String charactUUID, int propertyType) {
        BluetoothGattCharacteristic characteristic = getCharact(serviceUUID, charactUUID);
        if (characteristic != null) {
            int charaProp = characteristic.getProperties();
            return (charaProp & propertyType) > 0;
        } else {
            return false;
        }
    }

    /**
     * notify
     */
    public void notify(String serviceUUID, String notifyUUID, NotifyCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleNotifyCallback can not be Null!");
        }
        PeripheralController controller = newPeripheralController();
        if (controller != null) {
            controller.withUUIDString(serviceUUID, notifyUUID)
                    .enableCharacteristicNotify(callback, notifyUUID);
        }
    }

    /**
     * indicate
     */
    public void indicate(String serviceUUID, String indicateUUID, IndicateCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleIndicateCallback can not be Null!");
        }
        PeripheralController controller = newPeripheralController();
        if (controller != null) {
            controller.withUUIDString(serviceUUID, indicateUUID)
                    .enableCharacteristicIndicate(callback, indicateUUID);
        }
    }

    /**
     * stop notify, remove callback
     */
    public boolean stopNotify(String serviceUUID, String notifyUUID) {
        boolean success = false;
        PeripheralController controller = newPeripheralController();
        if (controller != null) {
            success = controller.withUUIDString(serviceUUID, notifyUUID)
                    .disableCharacteristicNotify();
            if (success) {
                removeNotifyCallback(notifyUUID);
            }
        }
        return success;
    }

    /**
     * stop indicate, remove callback
     */
    public boolean stopIndicate(String serviceUUID, String indicateUUID) {
        boolean success = false;
        PeripheralController controller = newPeripheralController();
        if (controller != null) {
            success = controller.withUUIDString(serviceUUID, indicateUUID)
                    .disableCharacteristicIndicate();
            if (success) {
                removeIndicateCallback(indicateUUID);
            }
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
        PeripheralController controller = newPeripheralController();
        if (controller != null) {
            controller.withUUIDString(serviceUUID, writeUUID)
                    .writeCharacteristic(data, callback, writeUUID);
        }
    }

    /**
     * read
     */
    public void read(String serviceUUID, String readUUID, ReadCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleReadCallback can not be Null!");
        }

        PeripheralController controller = newPeripheralController();
        if (controller != null) {
            controller.withUUIDString(serviceUUID, readUUID)
                    .readCharacteristic(callback, readUUID);
        }
    }

    /**
     * read Rssi
     */
    public void readRssi(RssiCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleRssiCallback can not be Null!");
        }
        PeripheralController controller = newPeripheralController();
        if (controller != null) {
            controller.readRemoteRssi(callback);
        }
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

        PeripheralController controller = newPeripheralController();
        if (controller != null) {
            controller.setMtu(mtu, callback);
        }
    }

    private void printCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            return;
        }
        String uuid, value;
        uuid = characteristic.getUuid() != null ? characteristic.getUuid().toString() : "null";
        value = characteristic.getValue() != null ? HexUtil.encodeHexStr(characteristic.getValue()) : "null";
        EasyLog.d("Characteristic uuid：%s  value: %s", uuid, value);
    }

    private void printDescriptor(BluetoothGattDescriptor descriptor) {
        if (descriptor == null) {
            return;
        }
        String uuid, value;
        uuid = descriptor.getUuid() != null ? descriptor.getUuid().toString() : "null";
        value = descriptor.getValue() != null ? HexUtil.encodeHexStr(descriptor.getValue()) : "null";
        EasyLog.d("Descriptor uuid：%s  value: %s", uuid, value);
    }

    private void handleConnectRetry(final int status) {
        if (mConnectRetryCount > 0 && (status == 0x3E | status == 0x85)) {
            getMainHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothGatt = connectGatt(false);
                    if (mBluetoothGatt == null) {
                        handleConnectFail(status);
                        mConnectRetryCount = 0;
                    } else {
                        String address = mBluetoothGatt.getDevice().getAddress();
                        EasyLog.i("Retry connect device mac:%s status:%d", address, status);
                        mConnectRetryCount--;
                    }
                }
            }, DEFAULT_DELAY_RETRY_CONNECT);
        } else {
            handleConnectFail(status);
        }
    }

    private void sendDiscoverServiceMsg(BluetoothGatt gatt) {
        Message msg = getMainHandler().obtainMessage(MSG_DISCOVER_SERVICE, gatt);
        getMainHandler().sendMessageDelayed(msg, DEFAULT_DELAY_DISCOVER_SERVICE);
    }

    private void discoverServiceMsgInit() {
        getMainHandler().removeMessages(MSG_DISCOVER_SERVICE);
    }

    private void handleConnectFail(int status) {
        CentralManager.getInstance().getMultiplePeripheralController().removePeripheral(Peripheral.this);
        mConnectState = ConnectionState.CONNECT_FAILURE;
        sendMsgToMainH(MSG_CONNECT_FAILURE, status, 0, mConnectStateCallback);
    }

    private void handleDisconnect(final int status) {
        CentralManager.getInstance().getMultiplePeripheralController().removePeripheral(Peripheral.this);
        mConnectState = ConnectionState.CONNECT_DISCONNECT;
        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                if (mConnectStateCallback != null) {
                    mConnectStateCallback.onDisConnected(isActivityDisconnect, getAddress(), status);
                }
            }
        });
    }

    private BluetoothGattCallback coreGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            /**
             * Status Code Description:
             * 0x3E(62): connection fail to establish
             * 0x85(133): GATT_ERROR
             * 0x101(257): no connection to cancel
             */
            super.onConnectionStateChange(gatt, status, newState);
            EasyLog.i("GattCallback：Connection State Change\nstatus: %d  newState: %d  currentThread: %d",
                    status, newState, Thread.currentThread().getId());

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                sendDiscoverServiceMsg(gatt);
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                if (gatt != null) {
                    gatt.close();
                }
                if (mConnectState == ConnectionState.CONNECT_CONNECTING) {
                    discoverServiceMsgInit();
                    handleConnectRetry(status);
                } else if (mConnectState == ConnectionState.CONNECT_CONNECTED) {
                    handleDisconnect(status);
                }
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            super.onServicesDiscovered(gatt, status);
            EasyLog.i("GattCallback：services discovered\nstatus: %d  currentThread: %d",
                    status, Thread.currentThread().getId());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBluetoothGatt = gatt;
                mConnectState = ConnectionState.CONNECT_CONNECTED;
                isActivityDisconnect = false;
                CentralManager.getInstance().getMultiplePeripheralController().addPeripheral(Peripheral.this);
                getMainHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (mConnectStateCallback != null) {
                            mConnectStateCallback.onConnectSuccess(getAddress(), status);
                        }
                    }
                });
            } else {
                if (gatt != null) {
                    gatt.close();
                }
                handleConnectFail(status);
            }
        }

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            EasyLog.i("GattCallback：onCharacteristicChanged ");
            printCharacteristic(characteristic);

            final NotifyCallback notifyCallback = findNotifyCallback(characteristic.getUuid().toString());
            getMainHandler().post(new Runnable() {
                @Override
                public void run() {
                    if (notifyCallback != null) {
                        notifyCallback.onCharacteristicChanged(characteristic.getValue());
                    }
                }
            });

            final IndicateCallback indicateCallback = findIndicateCallback(characteristic.getUuid().toString());
            getMainHandler().post(new Runnable() {
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
            EasyLog.i("GattCallback：onCharacteristicWrite ");
            printCharacteristic(characteristic);

            final WriteCallback writeCallback = findWriteCallback(characteristic.getUuid().toString());
            if (writeCallback != null) {
                writeCallback.getPeripheralConnector().writeMsgInit();
                sendMsgToMainH(MSG_WRITE_CALLBACK, status, 0, writeCallback);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            EasyLog.i("GattCallback：onCharacteristicRead ");
            printCharacteristic(characteristic);

            final ReadCallback readCallback = findReadCallback(characteristic.getUuid().toString());
            if (readCallback != null) {
                readCallback.getPeripheralConnector().readMsgInit();
                getMainHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            readCallback.onReadSuccess(characteristic.getValue());
                        } else {
                            readCallback.onReadFailure(new GattException(status));
                        }
                    }
                });
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, final int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            EasyLog.i("GattCallback：onDescriptorWrite ");
            printDescriptor(descriptor);

            String uuid = descriptor.getCharacteristic().getUuid().toString();
            final NotifyCallback notifyCallback = findNotifyCallback(uuid);
            if (notifyCallback != null) {
                notifyCallback.getPeripheralConnector().notifyMsgInit();
                sendMsgToMainH(MSG_NOTIFY_CALLBACK, status, 0, notifyCallback);
            }

            final IndicateCallback indicateCallback = findIndicateCallback(uuid);
            if (indicateCallback != null) {
                indicateCallback.getPeripheralConnector().indicateMsgInit();
                sendMsgToMainH(MSG_INDICATE_CALLBACK, status, 0, indicateCallback);
            }

        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            EasyLog.i("GattCallback：onDescriptorRead ");
            printDescriptor(descriptor);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, final int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            EasyLog.i("GattCallback：onReadRemoteRssi value: %d  status: %d", rssi, status);
            if (mRssiCallback != null) {
                mRssiCallback.getPeripheralConnector().rssiMsgInit();
                sendMsgToMainH(MSG_RSSI_CALLBACK, status, rssi, mRssiCallback);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, final int mtu, final int status) {
            super.onMtuChanged(gatt, mtu, status);
            EasyLog.i("GattCallback：onMtuChanged value: %d  status: %d", mtu, status);
            if (mMtuChangedCallback != null) {
                mMtuChangedCallback.getPeripheralConnector().mtuChangedMsgInit();
                sendMsgToMainH(MSG_MTU_CHANGE, status, mtu, mMtuChangedCallback);
            }
        }
    };
}
