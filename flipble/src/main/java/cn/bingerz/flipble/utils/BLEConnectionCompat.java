package cn.bingerz.flipble.utils;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.content.Context;
import android.os.Build;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import cn.bingerz.flipble.central.CentralManager;

/**
 * @author hanson
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BLEConnectionCompat {

    private final Context context;

    public BLEConnectionCompat(Context context) {
        this.context = context;
    }

    public BluetoothGattCompat connectGatt(BluetoothDevice remoteDevice, boolean autoConnect, BluetoothGattCallback bluetoothGattCallback) {

        if (remoteDevice == null) {
            return null;
        }

        /**
         * Issue that caused a race condition mentioned below was fixed in 7.0.0_r1
         * https://android.googlesource.com/platform/frameworks/base/+/android-7.0.0_r1/core/java/android/bluetooth/BluetoothGatt.java#649
         * compared to
         * https://android.googlesource.com/platform/frameworks/base/+/android-6.0.1_r72/core/java/android/bluetooth/BluetoothGatt.java#739
         * issue: https://android.googlesource.com/platform/frameworks/base/+/d35167adcaa40cb54df8e392379dfdfe98bcdba2%5E%21/#F0
         */
        if (GeneralUtil.isGreaterOrEqual7_0() || !autoConnect) {
            return connectGattCompat(bluetoothGattCallback, remoteDevice, autoConnect);
        }

        /**
         * Some implementations of Bluetooth Stack have a race condition where autoConnect flag
         * is not properly set before calling connectGatt. That's the reason for using reflection
         * to set the flag manually.
         */

        try {
            EasyLog.v("Trying to connectGatt using reflection.");
            Object iBluetoothGatt = getIBluetoothGatt(getIBluetoothManager());

            if (iBluetoothGatt == null) {
                EasyLog.w("Couldn't get iBluetoothGatt object");
                return connectGattCompat(bluetoothGattCallback, remoteDevice, true);
            }

            BluetoothGattCompat bluetoothGattCompat = createBluetoothGatt(iBluetoothGatt, remoteDevice);

            if (bluetoothGattCompat == null || bluetoothGattCompat.getBluetoothGatt() == null) {
                EasyLog.w("Couldn't create BluetoothGatt object");
                return connectGattCompat(bluetoothGattCallback, remoteDevice, true);
            }

            boolean connectedSuccessfully = connectUsingReflection(bluetoothGattCompat.getBluetoothGatt(),
                                                                    bluetoothGattCallback, true);

            if (!connectedSuccessfully) {
                EasyLog.w("Connection using reflection failed, closing gatt");
                bluetoothGattCompat.close();
            }
            return bluetoothGattCompat;
        } catch (NoSuchMethodException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException
                | InstantiationException
                | NoSuchFieldException exception) {
            EasyLog.w("Error during reflection", exception);
            return connectGattCompat(bluetoothGattCallback, remoteDevice, true);
        }
    }

    private BluetoothGattCompat connectGattCompat(BluetoothGattCallback callback, BluetoothDevice device, boolean autoConnect) {
        EasyLog.v("Connecting without reflection");
        BluetoothGattCompat gattCompat = null;
        if (CentralManager.getInstance().isBluetoothGranted()) {
            BluetoothGatt gatt;
            try {
                if (GeneralUtil.isGreaterOrEqual6_0()) {
                    gatt = device.connectGatt(context, autoConnect, callback, BluetoothDevice.TRANSPORT_LE);
                } else {
                    gatt = device.connectGatt(context, autoConnect, callback);
                }
                gattCompat = new BluetoothGattCompat(gatt);
            } catch (Exception e) {
                EasyLog.e("Error during connectGattCompat", e);
            }
        } else {
            EasyLog.e("Connect device fail, need grant BLUETOOTH_CONNECT");
        }
        return gattCompat;
    }

    private boolean connectUsingReflection(BluetoothGatt bluetoothGatt, BluetoothGattCallback bluetoothGattCallback, boolean autoConnect)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException, NoSuchFieldException {
        EasyLog.v("Connecting using reflection");
        setAutoConnectValue(bluetoothGatt, autoConnect);
        Method connectMethod = bluetoothGatt.getClass().getDeclaredMethod("connect", Boolean.class, BluetoothGattCallback.class);
        connectMethod.setAccessible(true);
        return (Boolean) (connectMethod.invoke(bluetoothGatt, true, bluetoothGattCallback));
    }

    @TargetApi(Build.VERSION_CODES.M)
    private BluetoothGattCompat createBluetoothGatt(Object iBluetoothGatt, BluetoothDevice remoteDevice)
            throws IllegalAccessException, InvocationTargetException, InstantiationException {
        Constructor<?> constructor = BluetoothGatt.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        EasyLog.v("Found constructor with args count=%d", constructor.getParameterTypes().length);
        BluetoothGatt gatt;
        if (constructor.getParameterTypes().length == 4) {
            gatt = (BluetoothGatt) (constructor.newInstance(context, iBluetoothGatt, remoteDevice, BluetoothDevice.TRANSPORT_LE));
        } else {
            gatt = (BluetoothGatt) (constructor.newInstance(context, iBluetoothGatt, remoteDevice));
        }
        return new BluetoothGattCompat(gatt);
    }

    private Object getIBluetoothGatt(Object iBluetoothManager)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        if (iBluetoothManager == null) {
            return null;
        }

        Method getBluetoothGattMethod = getMethodFromClass(iBluetoothManager.getClass(), "getBluetoothGatt");
        return getBluetoothGattMethod.invoke(iBluetoothManager);
    }

    private Object getIBluetoothManager() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            return null;
        }

        Method getBluetoothManagerMethod = getMethodFromClass(bluetoothAdapter.getClass(), "getBluetoothManager");
        return getBluetoothManagerMethod.invoke(bluetoothAdapter);
    }

    private Method getMethodFromClass(Class<?> cls, String methodName) throws NoSuchMethodException {
        Method method = cls.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method;
    }

    private void setAutoConnectValue(BluetoothGatt bluetoothGatt, boolean autoConnect) throws NoSuchFieldException, IllegalAccessException {
        Field autoConnectField = bluetoothGatt.getClass().getDeclaredField("mAutoConnect");
        autoConnectField.setAccessible(true);
        autoConnectField.setBoolean(bluetoothGatt, autoConnect);
    }
}
