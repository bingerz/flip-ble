package cn.bingerz.flipble.utils;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Reference:https://blog.csdn.net/u014418171/article/details/81219297
 */
public class BLEHackingMethod {

    public static final int CONNECTION_STATE_DISCONNECTED = 0;
    public static final int CONNECTION_STATE_CONNECTED = 1;
    public static final int CONNECTION_STATE_UN_SUPPORT = -1;

    private static Object getIBluetoothManager() throws NoSuchMethodException,
            InvocationTargetException, IllegalAccessException {
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

    public static Field getFieldFromClass(Class<?> clazz, String name) throws NoSuchFieldException {
        Field declaredField = clazz.getDeclaredField(name);
        declaredField.setAccessible(true);
        return declaredField;
    }

    public static Field getDeclaredField(Object obj, String name) throws NoSuchFieldException {
        return getFieldFromClass(obj.getClass(), name);
    }

    /**
     * Release all scan client
     * @return
     */
    public static boolean releaseAllScanClient() {
        try {
            Object mIBluetoothManager = getIBluetoothManager();
            if (mIBluetoothManager == null) {
                return false;
            }
            Object iGatt = getIBluetoothGatt(mIBluetoothManager);
            if (iGatt == null) {
                return false;
            }
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

    /**
     * hack get mClientIf value
     * @param callback
     * @return
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean isScanClientInitialize(ScanCallback callback) {
        try {
            Field mLeScanClientsField = getDeclaredField(BluetoothLeScanner.class, "mLeScanClients");
            //HashMap<ScanCallback, BleScanCallbackWrapper>()
            HashMap callbackList = (HashMap) mLeScanClientsField.get(BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner());
            int size = callbackList == null ? 0 : callbackList.size();
            if (size > 0) {
                Iterator iterator = callbackList.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry entry = (Map.Entry) iterator.next();
                    Object key = entry.getKey();
                    Object val = entry.getValue();
                    if (val != null && key != null && key == callback) {
                        int mClientIf = 0;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Field mScannerIdField = getDeclaredField(val, "mScannerId");
                            mClientIf = mScannerIdField.getInt(val);

                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Field mClientIfField = getDeclaredField(val, "mClientIf");
                            mClientIf = mClientIfField.getInt(val);
                        }
                        System.out.println("mClientIf=" + mClientIf);
                        return true;
                    }
                }
            } else {
                if (callback != null) {
                    return false;
                }
            }
        } catch (Exception ignored) {

        }
        return true;
    }
}
