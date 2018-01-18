package cn.bingerz.flipble.bluetoothle;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.os.Handler;
import android.os.Message;

import java.util.UUID;

import cn.bingerz.flipble.CentralManager;
import cn.bingerz.flipble.callback.IndicateCallback;
import cn.bingerz.flipble.callback.MtuChangedCallback;
import cn.bingerz.flipble.callback.NotifyCallback;
import cn.bingerz.flipble.callback.ReadCallback;
import cn.bingerz.flipble.callback.RssiCallback;
import cn.bingerz.flipble.callback.WriteCallback;
import cn.bingerz.flipble.exception.OtherException;
import cn.bingerz.flipble.exception.TimeoutException;

/**
 * Created by hanson on 09/01/2018.
 * be sure main thread
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class PeripheralController {

    private static final String UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    private static final int MSG_NOTIFY_CHA = 0x11;
    private static final int MSG_INDICATE_DES = 0x12;
    private static final int MSG_WRITE_CHA = 0x13;
    private static final int MSG_READ_CHA = 0x14;
    private static final int MSG_READ_RSSI = 0x15;
    private static final int MSG_SET_MTU = 0x16;


    private Peripheral mPeripheral;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattService service;
    private BluetoothGattCharacteristic characteristic;

    private Handler handler = new MyHandler();

    private static final class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_NOTIFY_CHA:
                    NotifyCallback notifyCallback = (NotifyCallback) msg.obj;
                    if (notifyCallback != null)
                        notifyCallback.onNotifyFailure(new TimeoutException());
                    msg.obj = null;
                    break;

                case MSG_INDICATE_DES:
                    IndicateCallback indicateCallback = (IndicateCallback) msg.obj;
                    if (indicateCallback != null)
                        indicateCallback.onIndicateFailure(new TimeoutException());
                    msg.obj = null;
                    break;

                case MSG_WRITE_CHA:
                    WriteCallback writeCallback = (WriteCallback) msg.obj;
                    if (writeCallback != null)
                        writeCallback.onWriteFailure(new TimeoutException());
                    msg.obj = null;
                    break;

                case MSG_READ_CHA:
                    ReadCallback readCallback = (ReadCallback) msg.obj;
                    if (readCallback != null)
                        readCallback.onReadFailure(new TimeoutException());
                    msg.obj = null;
                    break;

                case MSG_READ_RSSI:
                    RssiCallback rssiCallback = (RssiCallback) msg.obj;
                    if (rssiCallback != null)
                        rssiCallback.onRssiFailure(new TimeoutException());
                    msg.obj = null;
                    break;

                case MSG_SET_MTU:
                    MtuChangedCallback mtuChangedCallback = (MtuChangedCallback) msg.obj;
                    if (mtuChangedCallback != null)
                        mtuChangedCallback.onSetMTUFailure(new TimeoutException());
                    msg.obj = null;
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    public PeripheralController(Peripheral peripheral) {
        this.mPeripheral = peripheral;
        this.mBluetoothGatt = mPeripheral.getBluetoothGatt();
    }


    public PeripheralController withUUID(UUID serviceUUID, UUID charactUUID) {
        if (serviceUUID != null && mBluetoothGatt != null) {
            service = mBluetoothGatt.getService(serviceUUID);
        }

        if (service != null && charactUUID != null) {
            characteristic = service.getCharacteristic(charactUUID);
        }

        return this;
    }

    private UUID formUUID(String uuid) {
        return uuid == null ? null : UUID.fromString(uuid);
    }

    public PeripheralController withUUIDString(String serviceUUID, String charactUUID) {
        return withUUID(formUUID(serviceUUID), formUUID(charactUUID));
    }

    /**
     * notify
     */
    public void enableCharacteristicNotify(NotifyCallback notifyCallback, String notifyUUID) {
        if (characteristic != null
                && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {

            handleCharacteristicNotifyCallback(notifyCallback, notifyUUID);
            setCharacteristicNotification(mBluetoothGatt, characteristic, true, notifyCallback);
        } else {
            if (notifyCallback != null)
                notifyCallback.onNotifyFailure(new OtherException("this characteristic not support notify!"));
        }
    }

    /**
     * stop notify
     */
    public boolean disableCharacteristicNotify() {
        if (characteristic != null
                && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            return setCharacteristicNotification(mBluetoothGatt, characteristic, false, null);
        } else {
            return false;
        }
    }

    /**
     * notify setting
     */
    private boolean setCharacteristicNotification(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                                  boolean enable, NotifyCallback notifyCallback) {
        if (gatt == null || characteristic == null) {
            notifyMsgInit();
            if (notifyCallback != null)
                notifyCallback.onNotifyFailure(new OtherException("gatt or characteristic equal null"));
            return false;
        }

        boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
        if (!success1) {
            notifyMsgInit();
            if (notifyCallback != null)
                notifyCallback.onNotifyFailure(new OtherException("gatt setCharacteristicNotification fail"));
            return false;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
        if (descriptor == null) {
            notifyMsgInit();
            if (notifyCallback != null)
                notifyCallback.onNotifyFailure(new OtherException("descriptor equals null"));
            return false;
        } else {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            boolean success2 = gatt.writeDescriptor(descriptor);
            if (!success2) {
                notifyMsgInit();
                if (notifyCallback != null)
                    notifyCallback.onNotifyFailure(new OtherException("gatt writeDescriptor fail"));
            }
            return success2;
        }
    }

    /**
     * indicate
     */
    public void enableCharacteristicIndicate(IndicateCallback indicateCallback, String indicateUUID) {
        if (characteristic != null
                && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            handleCharacteristicIndicateCallback(indicateCallback, indicateUUID);
            setCharacteristicIndication(mBluetoothGatt, characteristic, true, indicateCallback);
        } else {
            if (indicateCallback != null)
                indicateCallback.onIndicateFailure(new OtherException("this characteristic not support indicate!"));
        }
    }


    /**
     * stop indicate
     */
    public boolean disableCharacteristicIndicate() {
        if (characteristic != null
                && (characteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            return setCharacteristicIndication(mBluetoothGatt, characteristic, false, null);
        } else {
            return false;
        }
    }

    /**
     * indicate setting
     */
    private boolean setCharacteristicIndication(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                                boolean enable, IndicateCallback indicateCallback) {
        if (gatt == null || characteristic == null) {
            indicateMsgInit();
            if (indicateCallback != null)
                indicateCallback.onIndicateFailure(new OtherException("gatt or characteristic equal null"));
            return false;
        }

        boolean success1 = gatt.setCharacteristicNotification(characteristic, enable);
        if (!success1) {
            indicateMsgInit();
            if (indicateCallback != null)
                indicateCallback.onIndicateFailure(new OtherException("gatt setCharacteristicNotification fail"));
            return false;
        }

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(formUUID(UUID_CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR));
        if (descriptor == null) {
            indicateMsgInit();
            if (indicateCallback != null)
                indicateCallback.onIndicateFailure(new OtherException("descriptor equals null"));
            return false;
        } else {
            descriptor.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE :
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            boolean success2 = gatt.writeDescriptor(descriptor);
            if (!success2) {
                indicateMsgInit();
                if (indicateCallback != null)
                    indicateCallback.onIndicateFailure(new OtherException("gatt writeDescriptor fail"));
            }
            return success2;
        }
    }

    /**
     * write
     */
    public void writeCharacteristic(byte[] data, WriteCallback writeCallback, String writeUUID) {
        if (data == null || data.length <= 0) {
            if (writeCallback != null)
                writeCallback.onWriteFailure(new OtherException("the data to be written is empty"));
            return;
        }

        if (characteristic == null
                || (characteristic.getProperties() & (BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) == 0) {
            if (writeCallback != null)
                writeCallback.onWriteFailure(new OtherException("this characteristic not support write!"));
            return;
        }

        if (characteristic.setValue(data)) {
            handleCharacteristicWriteCallback(writeCallback, writeUUID);
            if (!mBluetoothGatt.writeCharacteristic(characteristic)) {
                writeMsgInit();
                if (writeCallback != null)
                    writeCallback.onWriteFailure(new OtherException("gatt writeCharacteristic fail"));
            }
        } else {
            if (writeCallback != null)
                writeCallback.onWriteFailure(new OtherException("Updates the locally stored value of this characteristic fail"));
        }
    }

    /**
     * read
     */
    public void readCharacteristic(ReadCallback readCallback, String readUUID) {
        if (characteristic != null
                && (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {

            handleCharacteristicReadCallback(readCallback, readUUID);
            if (!mBluetoothGatt.readCharacteristic(characteristic)) {
                readMsgInit();
                if (readCallback != null)
                    readCallback.onReadFailure(new OtherException("gatt readCharacteristic fail"));
            }
        } else {
            if (readCallback != null)
                readCallback.onReadFailure(new OtherException("this characteristic not support read!"));
        }
    }

    /**
     * rssi
     */
    public void readRemoteRssi(RssiCallback rssiCallback) {
        handleRSSIReadCallback(rssiCallback);
        if (!mBluetoothGatt.readRemoteRssi()) {
            rssiMsgInit();
            if (rssiCallback != null)
                rssiCallback.onRssiFailure(new OtherException("gatt readRemoteRssi fail"));
        }
    }

    /**
     * set mtu
     */
    public void setMtu(int requiredMtu, MtuChangedCallback mtuChangedCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            handleSetMtuCallback(mtuChangedCallback);
            if (!mBluetoothGatt.requestMtu(requiredMtu)) {
                mtuChangedMsgInit();
                if (mtuChangedCallback != null)
                    mtuChangedCallback.onSetMTUFailure(new OtherException("gatt requestMtu fail"));
            }
        } else {
            if (mtuChangedCallback != null)
                mtuChangedCallback.onSetMTUFailure(new OtherException("API level lower than 21"));
        }
    }


    /**************************************** Handle call back ******************************************/

    /**
     * notify
     */
    private void handleCharacteristicNotifyCallback(NotifyCallback notifyCallback, String notifyUUID) {
        if (notifyCallback != null) {
            notifyMsgInit();
            notifyCallback.setPeripheralConnector(this);
            notifyCallback.setKey(notifyUUID);
            mPeripheral.addNotifyCallback(notifyUUID, notifyCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_NOTIFY_CHA, notifyCallback),
                    CentralManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * indicate
     */
    private void handleCharacteristicIndicateCallback(IndicateCallback indicateCallback, String indicateUUID) {
        if (indicateCallback != null) {
            indicateMsgInit();
            indicateCallback.setPeripheralConnector(this);
            indicateCallback.setKey(indicateUUID);
            mPeripheral.addIndicateCallback(indicateUUID, indicateCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_INDICATE_DES, indicateCallback),
                    CentralManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * write
     */
    private void handleCharacteristicWriteCallback(WriteCallback writeCallback, String writeUUID) {
        if (writeCallback != null) {
            writeMsgInit();
            writeCallback.setPeripheralConnector(this);
            writeCallback.setKey(writeUUID);
            mPeripheral.addWriteCallback(writeUUID, writeCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_WRITE_CHA, writeCallback),
                    CentralManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * read
     */
    private void handleCharacteristicReadCallback(ReadCallback readCallback, String readUUID) {
        if (readCallback != null) {
            readMsgInit();
            readCallback.setPeripheralConnector(this);
            readCallback.setKey(readUUID);
            mPeripheral.addReadCallback(readUUID, readCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_READ_CHA, readCallback),
                    CentralManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * rssi
     */
    private void handleRSSIReadCallback(final RssiCallback rssiCallback) {
        if (rssiCallback != null) {
            rssiMsgInit();
            rssiCallback.setPeripheralConnector(this);
            mPeripheral.addRssiCallback(rssiCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_READ_RSSI, rssiCallback),
                    CentralManager.getInstance().getOperateTimeout());
        }
    }

    /**
     * set mtu
     */
    private void handleSetMtuCallback(final MtuChangedCallback mtuChangedCallback) {
        if (mtuChangedCallback != null) {
            mtuChangedMsgInit();
            mtuChangedCallback.setPeripheralConnector(this);
            mPeripheral.addMtuChangedCallback(mtuChangedCallback);
            handler.sendMessageDelayed(handler.obtainMessage(MSG_SET_MTU, mtuChangedCallback),
                    CentralManager.getInstance().getOperateTimeout());
        }
    }

    public void notifyMsgInit() {
        handler.removeMessages(MSG_NOTIFY_CHA);
    }

    public void indicateMsgInit() {
        handler.removeMessages(MSG_INDICATE_DES);
    }

    public void writeMsgInit() {
        handler.removeMessages(MSG_WRITE_CHA);
    }

    public void readMsgInit() {
        handler.removeMessages(MSG_READ_CHA);
    }

    public void rssiMsgInit() {
        handler.removeMessages(MSG_READ_RSSI);
    }

    public void mtuChangedMsgInit() {
        handler.removeMessages(MSG_SET_MTU);
    }

}