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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
import cn.bingerz.flipble.peripheral.command.Command;
import cn.bingerz.flipble.scanner.ScanDevice;
import cn.bingerz.flipble.utils.BLEConnectionCompat;
import cn.bingerz.flipble.utils.EasyLog;
import cn.bingerz.flipble.utils.HexUtil;

/**
 * @author hanson
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class Peripheral {

    private static final int MSG_CONNECT_FAILURE    = 0x11;
    private static final int MSG_CONNECT_SUCCESS    = 0x12;
    private static final int MSG_DISCOVER_SERVICE   = 0x13;
    private static final int MSG_READ_CALLBACK      = 0x14;
    private static final int MSG_WRITE_CALLBACK     = 0x15;
    private static final int MSG_NOTIFY_CALLBACK    = 0x16;
    private static final int MSG_INDICATE_CALLBACK  = 0x17;
    private static final int MSG_RSSI_CALLBACK      = 0x18;
    private static final int MSG_MTU_CHANGE         = 0x19;
    private static final int MSG_RETRY_CONNECT      = 0x1A;

    private static final int DEFAULT_MTU = 23;
    private static final int DEFAULT_MAX_MTU = 512;
    private static final int DEFAULT_DELAY_CONNECT_EVENT = 600;
    private static final int DEFAULT_DELAY_DISCOVER_SERVICE = 600;

    private static final int DEFAULT_CONNECT_RETRY_COUNT = 2;
    private static final int DEFAULT_DELAY_CONNECT_RETRY = 500;

    private static final int DEFAULT_DELAY_NEXT_COMMAND = 500;

    private ConnectionState mConnectState = ConnectionState.CONNECT_IDLE;

    //Client actively performs the disconnect method
    private boolean isActivityDisconnect = false;

    private float mRssi;
    //卡尔曼滤波用的协方差估计值(Covariance estimation)
    private float mCov;

    private int mConnectRetryCount;

    private ScanDevice mDevice;
    private BluetoothGatt mBluetoothGatt;

    private final Object mStateLock = new Object();
    private Boolean mPeripheralBusy = false;

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
                case MSG_CONNECT_SUCCESS:
                    status = msg.arg1;
                    Peripheral peripheral = (Peripheral) msg.obj;
                    peripheral.handleConnectSuccess(status);
                    msg.obj = null;
                    break;
                case MSG_READ_CALLBACK:
                    //TODO because of too many parameters。
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
                case MSG_RETRY_CONNECT:
                    status = msg.arg1;
                    peripheral = (Peripheral) msg.obj;
                    if (peripheral != null) {
                        peripheral.handleRetryConnectCallback(status);
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

    private void sendMsgDelayedToMainH(int what, int arg1, int arg2, Object obj, long delayMillis) {
        Message msg = getMainHandler().obtainMessage(what, arg1, arg2, obj);
        getMainHandler().sendMessageDelayed(msg, delayMillis);
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

    public boolean isBusyState() {
        return mPeripheralBusy;
    }

    public void setBusyState() {
        synchronized (mPeripheralBusy) {
            mPeripheralBusy = true;
        }
    }

    public void resetBusyState() {
        synchronized (mPeripheralBusy) {
            mPeripheralBusy = false;
        }
        getMainHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                CentralManager.getInstance().getMultiplePeripheralController().executeNextCommand();
            }
        }, DEFAULT_DELAY_NEXT_COMMAND);
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
        mNotifyCallbackMap.remove(uuid);
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
        mIndicateCallbackMap.remove(uuid);
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
        mWriteCallbackMap.remove(uuid);
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
        mReadCallbackMap.remove(uuid);
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
                EasyLog.w("When Connect(true/false) is called, retrying Connect(true/false) is dangerous");
                closeBluetoothGatt();
            }
            EasyLog.i("Connect device=%s mac=%s autoConnect=%s", getName(), getAddress(), autoConnect);
            addConnectionStateCallback(callback);
            setBusyState();
            mBluetoothGatt = connectGatt(autoConnect);
            if (mBluetoothGatt != null) {
                synchronized (mStateLock) {
                    mConnectState = ConnectionState.CONNECT_CONNECTING;
                }
                mConnectRetryCount = DEFAULT_CONNECT_RETRY_COUNT;
                if (mConnectStateCallback != null) {
                    mConnectStateCallback.onStartConnect();
                }
                return true;
            } else {
                EasyLog.e("Connect device fail, BluetoothGatt is null.");
                resetBusyState();
            }
            return false;
        }
    }

    private BluetoothGatt connectGatt(boolean autoConnect) {
        BluetoothGatt bluetoothGatt = null;
        BluetoothDevice device = getBluetoothDevice();
        if (device == null) {
            EasyLog.i("Connect device fail, BluetoothDevice is null.");
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
            synchronized (mStateLock) {
                mConnectState = ConnectionState.CONNECT_DISCONNECTING;
            }
        }
        resetBusyState();
        getMainHandler().removeCallbacksAndMessages(null);
    }

    private synchronized void closeBluetoothGatt() {
        if (mBluetoothGatt != null) {
            //Phone Model: Samsung Galaxy S*/J* Android 5.1.1/6.0.1
            //java.lang.NullPointerException: Attempt to invoke virtual method
            //'android.os.Looper android.os.Handler.getLooper()' on a null object reference
            //android.os.Parcel.readException (Parcel.java:1626)
            //android.os.Parcel.readException (Parcel.java:1573)
            //android.bluetooth.IBluetoothGatt$Stub$Proxy.unregisterClient (IBluetoothGatt.java:1003)
            //android.bluetooth.BluetoothGatt.unregisterApp (BluetoothGatt.java:820)
            //android.bluetooth.BluetoothGatt.close (BluetoothGatt.java:759)
            try {
                mBluetoothGatt.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void destroy() {
        synchronized (mStateLock) {
            mConnectState = ConnectionState.CONNECT_IDLE;
        }
        //Add try catch code block, Binder(IPC) NullPointerException, Parcel.readException
        try {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        resetBusyState();
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

    private Command createCommand(int priority, int method, String serviceUUID, String charactUUID, byte[] data, Object callback) {
        return  new Command(priority, getAddress(), method, serviceUUID, charactUUID, data, callback);
    }

    public Command createNotify(int priority, String serviceUUID, String charactUUID, boolean isEnable, NotifyCallback callback) {
        byte[] data = isEnable ? Command.ENABLE : Command.DISABLE;
        return createCommand(priority, Command.Method.NOTIFY, serviceUUID, charactUUID, data, callback);
    }

    public Command createIndicate(int priority, String serviceUUID, String charactUUID, boolean isEnable, IndicateCallback callback) {
        byte[] data = isEnable ? Command.ENABLE : Command.DISABLE;
        return createCommand(priority, Command.Method.NOTIFY, serviceUUID, charactUUID, data, callback);
    }

    public Command createWrite(int priority, String serviceUUID, String charactUUID, byte[] data, WriteCallback callback) {
        return createCommand(priority, Command.Method.WRITE, serviceUUID, charactUUID, data, callback);
    }

    public Command createRead(int priority, String serviceUUID, String charactUUID, ReadCallback callback) {
        return createCommand(priority, Command.Method.READ, serviceUUID, charactUUID, null, callback);
    }

    public Command createReadRssi(int priority, RssiCallback callback) {
        return createCommand(priority, Command.Method.READ_RSSI, null, null, null, callback);
    }

    public Command createSetMtu(int priority, int mtu, MtuChangedCallback callback) {
        byte[] data = ByteBuffer.allocate(4).putInt(mtu).array();
        return createCommand(priority, Command.Method.SET_MTU, null, null, data, callback);
    }

    /**
     * notify
     * This operation will be performed immediately
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
     * stop notify, remove callback
     * This operation will be performed immediately
     */
    public boolean stopNotify(String serviceUUID, String notifyUUID) {
        boolean success = false;
        PeripheralController controller = newPeripheralController();
        if (controller != null) {
            success = controller.withUUIDString(serviceUUID, notifyUUID)
                    .disableCharacteristicNotify();
            if (success) {
                getMainHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resetBusyState();
                    }
                }, 2 * 1000);
                removeNotifyCallback(notifyUUID);
            }
        }
        return success;
    }

    /**
     * notify
     * Support priority and cache features
     * @param command
     */
    public void notify(Command command) {
        if (command == null || !command.isValid() ||
                command.getMethod() != Command.Method.NOTIFY ||
                !(command.getCallback() != null && command.getCallback() instanceof NotifyCallback)) {
            throw new IllegalArgumentException("BleNotify Command is invalid!");
        }

        NotifyCallback callback = (NotifyCallback) command.getCallback();
        if (CentralManager.getInstance().getMultiplePeripheralController().isContainBusyDevice()) {
            CentralManager.getInstance().getMultiplePeripheralController().cacheCommand(command);
        } else {
            String serviceUUID = command.getServiceUUID();
            String charactUUID = command.getCharactUUID();
            if (command.isEnable()) {
                notify(serviceUUID, charactUUID, callback);
            } else {
                stopNotify(serviceUUID, charactUUID);
            }
        }
    }

    /**
     * indicate
     * This operation will be performed immediately
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
     * stop indicate, remove callback
     * This operation will be performed immediately
     */
    public boolean stopIndicate(String serviceUUID, String indicateUUID) {
        boolean success = false;
        PeripheralController controller = newPeripheralController();
        if (controller != null) {
            success = controller.withUUIDString(serviceUUID, indicateUUID)
                    .disableCharacteristicIndicate();
            if (success) {
                getMainHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resetBusyState();
                    }
                }, 2 * 1000);
                removeIndicateCallback(indicateUUID);
            }
        }
        return success;
    }

    /**
     * indicate
     * Support priority and cache features
     * @param command
     */
    public void indicate(Command command) {
        if (command == null || !command.isValid() ||
                command.getMethod() != Command.Method.INDICATE ||
                !(command.getCallback() != null && command.getCallback() instanceof IndicateCallback)) {
            throw new IllegalArgumentException("BleIndicate Command is invalid!");
        }

        IndicateCallback callback = (IndicateCallback) command.getCallback();
        if (CentralManager.getInstance().getMultiplePeripheralController().isContainBusyDevice()) {
            CentralManager.getInstance().getMultiplePeripheralController().cacheCommand(command);
        } else {
            String serviceUUID = command.getServiceUUID();
            String charactUUID = command.getCharactUUID();
            if (command.isEnable()) {
                indicate(serviceUUID, charactUUID, callback);
            } else {
                stopIndicate(serviceUUID, charactUUID);
            }
        }
    }

    /**
     * write
     * This operation will be performed immediately
     */
    public void write(String serviceUUID, String writeUUID, byte[] data, WriteCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleWriteCallback can not be Null!");
        }

        if (data == null) {
            EasyLog.e("Write device fail, data is null.");
            callback.onWriteFailure(new OtherException("write data is null"));
            return;
        }

        if (data.length > 20) {
            EasyLog.w("Write device warning, data length > 20.");
        }
        PeripheralController controller = newPeripheralController();
        if (controller != null) {
            controller.withUUIDString(serviceUUID, writeUUID)
                    .writeCharacteristic(data, callback, writeUUID);
        }
    }

    /**
     * write
     * Support priority and cache features
     * @param command
     */
    public void write(Command command) {
        if (command == null || !command.isValid() ||
                command.getMethod() != Command.Method.WRITE ||
                !(command.getCallback() != null && command.getCallback() instanceof WriteCallback)) {
            throw new IllegalArgumentException("BleWrite Command is invalid");
        }

        WriteCallback callback = (WriteCallback) command.getCallback();
        if (command.getData() == null) {
            EasyLog.e("Write device fail, data is null.");
            callback.onWriteFailure(new OtherException("write data is null"));
            return;
        }
        if (command.getData().length > 20) {
            EasyLog.w("Write device warning, data length > 20.");
        }
        if (CentralManager.getInstance().getMultiplePeripheralController().isContainBusyDevice()) {
            CentralManager.getInstance().getMultiplePeripheralController().cacheCommand(command);
        } else {
            String serviceUUID = command.getServiceUUID();
            String charactUUID = command.getCharactUUID();
            PeripheralController controller = newPeripheralController();
            if (controller != null) {
                controller.withUUIDString(serviceUUID, charactUUID)
                        .writeCharacteristic(command.getData(), callback, charactUUID);
            }
        }
    }

    /**
     * read
     * This operation will be performed immediately
     */
    public void read(String serviceUUID, String readUUID, ReadCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleReadCallback is null");
        }

        PeripheralController controller = newPeripheralController();
        if (controller != null) {
            controller.withUUIDString(serviceUUID, readUUID)
                    .readCharacteristic(callback, readUUID);
        }
    }

    /**
     * read
     * Support priority and cache features
     * @param command
     */
    public void read(Command command) {
        if (command == null || !command.isValid() ||
                command.getMethod() != Command.Method.READ ||
                !(command.getCallback() != null && command.getCallback() instanceof ReadCallback)) {
            throw new IllegalArgumentException("BleRead Command is invalid");
        }

        ReadCallback callback = (ReadCallback) command.getCallback();
        if (CentralManager.getInstance().getMultiplePeripheralController().isContainBusyDevice()) {
            CentralManager.getInstance().getMultiplePeripheralController().cacheCommand(command);
        } else {
            String serviceUUID = command.getServiceUUID();
            String charactUUID = command.getCharactUUID();
            PeripheralController controller = newPeripheralController();
            if (controller != null) {
                controller.withUUIDString(serviceUUID, charactUUID)
                        .readCharacteristic(callback, charactUUID);
            }
        }
    }

    /**
     * read Rssi
     * This operation will be performed immediately
     */
    public void readRssi(RssiCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleRssiCallback is null");
        }
        PeripheralController controller = newPeripheralController();
        if (controller != null) {
            controller.readRemoteRssi(callback);
        }
    }

    /**
     * readRssi
     * Support priority and cache features
     * @param command
     */
    public void readRssi(Command command) {
        if (command == null || !command.isValid() ||
                command.getMethod() != Command.Method.READ_RSSI ||
                !(command.getCallback() != null && command.getCallback() instanceof RssiCallback)) {
            throw new IllegalArgumentException("BleReadRssi Command is invalid");
        }

        RssiCallback callback = (RssiCallback) command.getCallback();
        if (CentralManager.getInstance().getMultiplePeripheralController().isContainBusyDevice()) {
            CentralManager.getInstance().getMultiplePeripheralController().cacheCommand(command);
        } else {
            readRssi(callback);
        }
    }

    /**
     * set Mtu
     * This operation will be performed immediately
     */
    public void setMtu(int mtu, MtuChangedCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("BleMtuChangedCallback is null");
        }

        if (mtu > DEFAULT_MAX_MTU) {
            EasyLog.e("SetMtu should lower than 512");
            callback.onSetMTUFailure(new OtherException("SetMtu should lower than 512"));
            return;
        }

        if (mtu < DEFAULT_MTU) {
            EasyLog.e("SetMtu should higher than 23");
            callback.onSetMTUFailure(new OtherException("SetMtu should higher than 23"));
            return;
        }

        PeripheralController controller = newPeripheralController();
        if (controller != null) {
            controller.setMtu(mtu, callback);
        }
    }

    /**
     * setMtu
     * Support priority and cache features
     * @param command
     */
    public void setMtu(Command command) {
        if (command == null || !command.isValid() ||
                command.getMethod() != Command.Method.SET_MTU ||
                !(command.getCallback() != null && command.getCallback() instanceof MtuChangedCallback)) {
            throw new IllegalArgumentException("BleSetMtu Command is invalid");
        }

        MtuChangedCallback callback = (MtuChangedCallback) command.getCallback();
        if (command.getData() == null) {
            EasyLog.e("Set device mtu fail, data is null");
            callback.onSetMTUFailure(new OtherException("SetMtu data is null"));
            return;
        }
        if (CentralManager.getInstance().getMultiplePeripheralController().isContainBusyDevice()) {
            CentralManager.getInstance().getMultiplePeripheralController().cacheCommand(command);
        } else {
            int mtu = ByteBuffer.wrap(command.getData()).getInt();
            setMtu(mtu, callback);
        }
    }

    private void printCharacteristic(String tag, BluetoothGattCharacteristic charact, String status) {
        if (charact == null) {
            return;
        }
        String uuid, value;
        uuid = charact.getUuid() != null ? charact.getUuid().toString() : "null";
        value = charact.getValue() != null ? HexUtil.encodeHexStr(charact.getValue()) : "null";
        EasyLog.v("%s uuid=%s  value=%s  status=%s  currentThread=%d",
                    tag, uuid, value, status, Thread.currentThread().getId());
    }

    private void printDescriptor(String tag, BluetoothGattDescriptor descriptor, String status) {
        if (descriptor == null) {
            return;
        }
        String uuid, value;
        uuid = descriptor.getUuid() != null ? descriptor.getUuid().toString() : "null";
        value = descriptor.getValue() != null ? HexUtil.encodeHexStr(descriptor.getValue()) : "null";
        EasyLog.v("%s uuid=%s  value=%s  status=%s  currentThread=%d",
                    tag, uuid, value, status, Thread.currentThread().getId());
    }

    private void handleConnectRetry(final int status) {
        //蓝牙协议栈预定义错误码
        int GATT_ERROR = 0x85;
        int GATT_CONN_FAIL_ESTABLISH = 0x3E;
        if (mConnectRetryCount > 0 && (status == GATT_CONN_FAIL_ESTABLISH | status == GATT_ERROR)) {
            sendMsgDelayedToMainH(MSG_RETRY_CONNECT, status, 0, Peripheral.this, DEFAULT_DELAY_CONNECT_RETRY);
        } else {
            handleConnectFail(status);
        }
    }

    private void handleRetryConnectCallback(int status) {
        mBluetoothGatt = connectGatt(false);
        if (mBluetoothGatt == null) {
            handleConnectFail(status);
            mConnectRetryCount = 0;
        } else {
            String address = mBluetoothGatt.getDevice().getAddress();
            EasyLog.i("Retry connect device mac=%s status=%d", address, status);
            mConnectRetryCount--;
        }
    }

    private void sendDiscoverServiceMsg(BluetoothGatt gatt) {
        sendMsgDelayedToMainH(MSG_DISCOVER_SERVICE, 0, 0, gatt, DEFAULT_DELAY_DISCOVER_SERVICE);
    }

    private void discoverServiceMsgInit() {
        getMainHandler().removeMessages(MSG_DISCOVER_SERVICE);
    }

    private void handleConnectSuccess(int status) {
        if (mConnectStateCallback != null) {
            mConnectStateCallback.onConnectSuccess(Peripheral.this, status);
        }
    }

    private void handleConnectFail(int status) {
        CentralManager.getInstance().getMultiplePeripheralController().removePeripheral(Peripheral.this);
        synchronized (mStateLock) {
            mConnectState = ConnectionState.CONNECT_FAILURE;
        }
        sendMsgToMainH(MSG_CONNECT_FAILURE, status, 0, mConnectStateCallback);
        resetBusyState();
    }

    private void handleDisconnect(final int status) {
        CentralManager.getInstance().getMultiplePeripheralController().removePeripheral(Peripheral.this);
        synchronized (mStateLock) {
            mConnectState = ConnectionState.CONNECT_DISCONNECTED;
        }
        getMainHandler().post(new Runnable() {
            @Override
            public void run() {
                if (mConnectStateCallback != null) {
                    mConnectStateCallback.onDisConnected(isActivityDisconnect, getAddress(), status);
                }
            }
        });
        resetBusyState();
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
            EasyLog.i("GattCallback：ConnectionStateChange status=%d  newState=%d  currentThread=%d",
                        status, newState, Thread.currentThread().getId());

            if (newState == BluetoothGatt.STATE_CONNECTED) {
                mBluetoothGatt = gatt;
                sendDiscoverServiceMsg(gatt);
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                if (gatt != null) {
                    gatt.close();
                }
                synchronized (mStateLock) {
                    if (mConnectState == ConnectionState.CONNECT_CONNECTING) {
                        discoverServiceMsgInit();
                        handleConnectRetry(status);
                    } else if (mConnectState == ConnectionState.CONNECT_CONNECTED
                            || mConnectState == ConnectionState.CONNECT_DISCONNECTING) {
                        handleDisconnect(status);
                    }
                }
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            super.onServicesDiscovered(gatt, status);
            EasyLog.i("GattCallback：ServicesDiscovered status=%d  currentThread=%d",
                        status, Thread.currentThread().getId());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED);
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBluetoothGatt = gatt;
                ConnectionState prevState = mConnectState;
                synchronized (mStateLock) {
                    mConnectState = ConnectionState.CONNECT_CONNECTED;
                }
                isActivityDisconnect = false;
                CentralManager.getInstance().getMultiplePeripheralController().addPeripheral(Peripheral.this);
                if (prevState != mConnectState) {
                    sendMsgDelayedToMainH(MSG_CONNECT_SUCCESS, status, 0, Peripheral.this, DEFAULT_DELAY_CONNECT_EVENT);
                }
            } else {
                if (gatt != null) {
                    gatt.close();
                }
                handleConnectFail(status);
            }
            resetBusyState();
        }

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            printCharacteristic("GattCallback：onCharacteristicChanged", characteristic, "null");

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
            printCharacteristic("GattCallback：onCharacteristicWrite", characteristic, status + "");

            final WriteCallback writeCallback = findWriteCallback(characteristic.getUuid().toString());
            if (writeCallback != null) {
                resetBusyState();
                writeCallback.getPeripheralConnector().writeMsgInit();
                sendMsgToMainH(MSG_WRITE_CALLBACK, status, 0, writeCallback);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            printCharacteristic("GattCallback：onCharacteristicRead", characteristic, status + "");

            final ReadCallback readCallback = findReadCallback(characteristic.getUuid().toString());
            if (readCallback != null) {
                resetBusyState();
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
            printDescriptor("GattCallback：onDescriptorWrite", descriptor, status + "");

            String uuid = descriptor.getCharacteristic().getUuid().toString();
            final NotifyCallback notifyCallback = findNotifyCallback(uuid);
            if (notifyCallback != null) {
                resetBusyState();
                notifyCallback.getPeripheralConnector().notifyMsgInit();
                sendMsgToMainH(MSG_NOTIFY_CALLBACK, status, 0, notifyCallback);
            }

            final IndicateCallback indicateCallback = findIndicateCallback(uuid);
            if (indicateCallback != null) {
                resetBusyState();
                indicateCallback.getPeripheralConnector().indicateMsgInit();
                sendMsgToMainH(MSG_INDICATE_CALLBACK, status, 0, indicateCallback);
            }

        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            printDescriptor("GattCallback：onDescriptorRead", descriptor, status + "");
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, final int rssi, final int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            EasyLog.v("GattCallback：onReadRemoteRssi value=%d  status=%d", rssi, status);
            if (mRssiCallback != null) {
                resetBusyState();
                mRssiCallback.getPeripheralConnector().rssiMsgInit();
                sendMsgToMainH(MSG_RSSI_CALLBACK, status, rssi, mRssiCallback);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, final int mtu, final int status) {
            super.onMtuChanged(gatt, mtu, status);
            EasyLog.v("GattCallback：onMtuChanged value=%d  status=%d", mtu, status);
            if (mMtuChangedCallback != null) {
                resetBusyState();
                mMtuChangedCallback.getPeripheralConnector().mtuChangedMsgInit();
                sendMsgToMainH(MSG_MTU_CHANGE, status, mtu, mMtuChangedCallback);
            }
        }
    };
}
