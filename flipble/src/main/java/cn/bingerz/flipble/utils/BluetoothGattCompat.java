package cn.bingerz.flipble.utils;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.List;
import java.util.UUID;

import cn.bingerz.flipble.central.CentralManager;

/**
 * @author hanson
 */
public class BluetoothGattCompat {

    private BluetoothGatt bluetoothGatt;

    public BluetoothGattCompat(BluetoothGatt gatt) {
        this.bluetoothGatt = gatt;
    }

    public BluetoothGatt getBluetoothGatt() {
        return this.bluetoothGatt;
    }

    public void setBluetoothGatt(BluetoothGatt gatt) {
        this.bluetoothGatt = gatt;
    }

    public BluetoothDevice getDevice() {
        if (bluetoothGatt == null) {
            return null;
        }
        return bluetoothGatt.getDevice();
    }

    public boolean requestConnectionPriority(int connectionPriority) {
        boolean result = false;
        if (bluetoothGatt != null) {
            if (GeneralUtil.isBleSupportRequestPriority()) {
                if (CentralManager.getInstance().isBluetoothGranted()) {
                    result = bluetoothGatt.requestConnectionPriority(connectionPriority);
                } else if (GeneralUtil.isNeedBluetoothGrant()) {
                    EasyLog.e("Request connection priority fail, need grant BLUETOOTH_CONNECT");
                }
            }
        }
        return result;
    }

    public boolean discoverServices() {
        boolean result = false;
        if (bluetoothGatt != null) {
            if (CentralManager.getInstance().isBluetoothGranted()) {
                result = bluetoothGatt.discoverServices();
            } else if (GeneralUtil.isNeedBluetoothGrant()) {
                EasyLog.e("Discover services fail, need grant BLUETOOTH_CONNECT");
            }
        }
        return result;
    }

    public List<BluetoothGattService> getServices() {
        if (bluetoothGatt == null) {
            return null;
        }
        return bluetoothGatt.getServices();
    }

    public BluetoothGattService getService(UUID uuid) {
        if (bluetoothGatt == null) {
            return null;
        }
        return bluetoothGatt.getService(uuid);
    }

    public boolean setCharacteristicNotification(BluetoothGattCharacteristic charact, boolean enable) {
        boolean result = false;
        if (bluetoothGatt != null) {
            if (CentralManager.getInstance().isBluetoothGranted()) {
                result = bluetoothGatt.setCharacteristicNotification(charact, enable);
            } else if (GeneralUtil.isNeedBluetoothGrant()) {
                EasyLog.e("Set characteristic notification fail, need grant BLUETOOTH_CONNECT");
            }
        }
        return result;
    }

    public boolean writeDescriptor(BluetoothGattDescriptor descriptor) {
        boolean result = false;
        if (bluetoothGatt != null) {
            if (CentralManager.getInstance().isBluetoothGranted()) {
                result = bluetoothGatt.writeDescriptor(descriptor);
            } else if (GeneralUtil.isNeedBluetoothGrant()) {
                EasyLog.e("Write descriptor fail, need grant BLUETOOTH_CONNECT");
            }
        }
        return result;
    }

    public boolean readCharacteristic(BluetoothGattCharacteristic charact) {
        boolean result = false;
        if (bluetoothGatt != null) {
            if (CentralManager.getInstance().isBluetoothGranted()) {
                result = bluetoothGatt.readCharacteristic(charact);
            } else if (GeneralUtil.isNeedBluetoothGrant()) {
                EasyLog.e("Read characteristic fail, need grant BLUETOOTH_CONNECT");
            }
        }
        return result;
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic charact) {
        boolean result = false;
        if (bluetoothGatt != null) {
            if (CentralManager.getInstance().isBluetoothGranted()) {
                result = bluetoothGatt.writeCharacteristic(charact);
            } else if (GeneralUtil.isNeedBluetoothGrant()) {
                EasyLog.e("Write characteristic fail, need grant BLUETOOTH_CONNECT");
            }
        }
        return result;
    }

    public boolean readRemoteRssi() {
        boolean result = false;
        if (bluetoothGatt != null) {
            if (CentralManager.getInstance().isBluetoothGranted()) {
                result = bluetoothGatt.readRemoteRssi();
            } else if (GeneralUtil.isNeedBluetoothGrant()) {
                EasyLog.e("Read remote rssi fail, need grant BLUETOOTH_CONNECT");
            }
        }
        return result;
    }

    public boolean requestMtu(int mtu) {
        boolean result = false;
        if (bluetoothGatt != null) {
            if (CentralManager.getInstance().isBluetoothGranted()) {
                result = bluetoothGatt.requestMtu(mtu);
            } else if (GeneralUtil.isNeedBluetoothGrant()) {
                EasyLog.e("Request mtu fail, need grant BLUETOOTH_CONNECT");
            }
        }
        return result;
    }

    public void disconnect() {
        if (bluetoothGatt != null) {
            if (CentralManager.getInstance().isBluetoothGranted()) {
                bluetoothGatt.disconnect();
            } else if (GeneralUtil.isNeedBluetoothGrant()) {
                EasyLog.e("Disconnect fail, need grant BLUETOOTH_CONNECT");
            }
        }
    }

    public void close() {
        if (bluetoothGatt != null) {
            if (CentralManager.getInstance().isBluetoothGranted()) {
                bluetoothGatt.close();
            } else if (GeneralUtil.isNeedBluetoothGrant()) {
                EasyLog.e("Disconnect fail, need grant BLUETOOTH_CONNECT");
            }
        }
    }
}
