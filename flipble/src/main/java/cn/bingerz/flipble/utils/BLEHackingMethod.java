package cn.bingerz.flipble.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.os.Build;
import android.support.annotation.RequiresApi;

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
     * This Method only support Android 21 and above
     * @param address
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("PrivateApi")
    public static int getInternalConnectionState(String address) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return CONNECTION_STATE_UN_SUPPORT;
        }
        //Does not support OPPO phone, no solution.
        if(Build.MANUFACTURER.equalsIgnoreCase("OPPO")){
            return CONNECTION_STATE_UN_SUPPORT;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice remoteDevice = adapter.getRemoteDevice(address);
        Object mIBluetooth = null;
        try {
            Field sService = BluetoothDevice.class.getDeclaredField("sService");
            sService.setAccessible(true);
            mIBluetooth = sService.get(null);
        } catch (Exception e) {
            return CONNECTION_STATE_UN_SUPPORT;
        }
        if (mIBluetooth == null) return CONNECTION_STATE_UN_SUPPORT;

        boolean isConnected;
        try {
            Method isConnectedMethod = BluetoothDevice.class.getDeclaredMethod("isConnected");
            isConnectedMethod.setAccessible(true);
            isConnected = (Boolean) isConnectedMethod.invoke(remoteDevice);
            isConnectedMethod.setAccessible(false);
        } catch (Exception e) {
            try {
                Method getConnectionState = mIBluetooth.getClass().getDeclaredMethod("getConnectionState", BluetoothDevice.class);
                getConnectionState.setAccessible(true);
                int state = (Integer) getConnectionState.invoke(mIBluetooth, remoteDevice);
                getConnectionState.setAccessible(false);
                isConnected = state == CONNECTION_STATE_CONNECTED;
            } catch (Exception e1) {
                return CONNECTION_STATE_UN_SUPPORT;
            }
        }
        return isConnected ? CONNECTION_STATE_CONNECTED : CONNECTION_STATE_DISCONNECTED;
    }

    /**
     * Try BLE Service Enable or Disable
     * @param isEnable
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void setLeServiceEnable(boolean isEnable) {
        Object mIBluetooth;
        try {
            Field sService = BluetoothDevice.class.getDeclaredField("sService");
            sService.setAccessible(true);
            mIBluetooth = sService.get(null);
        } catch (Exception e) {
            return;
        }
        if (mIBluetooth == null) return;

        try {
            if (isEnable) {
                Method onLeServiceUp = mIBluetooth.getClass().getDeclaredMethod("onLeServiceUp");
                onLeServiceUp.setAccessible(true);
                onLeServiceUp.invoke(mIBluetooth);
            } else {
                Method onLeServiceUp = mIBluetooth.getClass().getDeclaredMethod("onBrEdrDown");
                onLeServiceUp.setAccessible(true);
                onLeServiceUp.invoke(mIBluetooth);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Release all scan client
     * @return
     */
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
