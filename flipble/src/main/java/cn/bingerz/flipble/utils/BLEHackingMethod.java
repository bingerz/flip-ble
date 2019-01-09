package cn.bingerz.flipble.utils;

import android.bluetooth.BluetoothAdapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BLEHackingMethod {

    private static Object getIBluetoothManager() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            return null;
        }

        Method getBluetoothManagerMethod = getDeclaredMethod(bluetoothAdapter, "getBluetoothManager");
        return getBluetoothManagerMethod.invoke(bluetoothAdapter);
    }

    private static Object getIBluetoothGatt(Object iBluetoothManager)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        if (iBluetoothManager == null) {
            return null;
        }

        Method getBluetoothGattMethod = getDeclaredMethod(iBluetoothManager, "getBluetoothGatt");
        return getBluetoothGattMethod.invoke(iBluetoothManager);
    }

    private static Method getMethodFromClass(Class<?> cls, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = cls.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private static Method getDeclaredMethod(Object obj, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        return getMethodFromClass(obj.getClass(), methodName, parameterTypes);
    }

    public static boolean releaseAllScanClient() {
        try {
            Object mIBluetoothManager = getIBluetoothManager();
            if (mIBluetoothManager == null) return false;
            Object iGatt = getIBluetoothGatt(mIBluetoothManager);
            if (iGatt == null) return false;

            Method unregisterClient = getDeclaredMethod(iGatt, "unregisterClient", int.class);
            Method stopScan;
            int type;
            try {
                type = 0;
                stopScan = getDeclaredMethod(iGatt, "stopScan", int.class, boolean.class);
            } catch (Exception e) {
                type = 1;
                stopScan = getDeclaredMethod(iGatt, "stopScan", int.class);
            }

            for (int mClientIf = 0; mClientIf <= 40; mClientIf++) {
                if (type == 0) {
                    try {
                        stopScan.invoke(iGatt, mClientIf, false);
                    } catch (Exception ignored) {
                    }
                }
                if (type == 1) {
                    try {
                        stopScan.invoke(iGatt, mClientIf);
                    } catch (Exception ignored) {
                    }
                }
                try {
                    unregisterClient.invoke(iGatt, mClientIf);
                } catch (Exception ignored) {
                }
            }
            stopScan.setAccessible(false);
            unregisterClient.setAccessible(false);
            getDeclaredMethod(iGatt, "unregAll").invoke(iGatt);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
