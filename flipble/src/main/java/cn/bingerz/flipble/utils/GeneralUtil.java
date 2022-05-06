package cn.bingerz.flipble.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

/**
 * @author hanson
 */
public class GeneralUtil {

    /**************************************************************************
     * Handle Phone info system version & model functions -
     *************************************************************************/
    public static boolean isGreaterThanOrEqual(int versionCode) {
        return Build.VERSION.SDK_INT >= versionCode;
    }

    public static boolean isGreaterThan(int versionCode) {
        return Build.VERSION.SDK_INT > versionCode;
    }

    public static boolean isEqualTo(int versionCode) {
        return Build.VERSION.SDK_INT == versionCode;
    }

    public static boolean isLessThan(int versionCode) {
        return Build.VERSION.SDK_INT < versionCode;
    }

    public static boolean isLessThanOrEqual(int versionCode) {
        return Build.VERSION.SDK_INT <= versionCode;
    }

    /**
     * System Version >= Android 4.3
     */
    public static boolean isGreaterOrEqual4_3() {
        return isGreaterThanOrEqual(Build.VERSION_CODES.JELLY_BEAN_MR2);
    }

    /**
     * System Version >= Android 5.0
     */
    public static boolean isGreaterOrEqual5_0() {
        return isGreaterThanOrEqual(Build.VERSION_CODES.LOLLIPOP);
    }

    /**
     * System Version >= Android 6.0
     */
    public static boolean isGreaterOrEqual6_0() {
        return isGreaterThanOrEqual(Build.VERSION_CODES.M);
    }

    /**
     * System Version >= Android 7.0
     */
    public static boolean isGreaterOrEqual7_0() {
        return isGreaterThanOrEqual(Build.VERSION_CODES.N);
    }

    /**
     * System Version >= Android 8.0
     */
    public static boolean isGreaterOrEqual8_0() {
        return isGreaterThanOrEqual(Build.VERSION_CODES.O);
    }

    /**
     * System Version >= Android 12.0
     */
    public static boolean isGreaterOrEqual12() {
        return isGreaterThanOrEqual(Build.VERSION_CODES.S);
    }

    /**************************************************************************
     * Handle Permission functions -
     *************************************************************************/
    public static int checkPermission(Context context, final String permission) {
        if (context == null) {
            throw new IllegalArgumentException("CheckPermission fail, context is null.");
        }
        int pid = android.os.Process.myPid();
        int uid = android.os.Process.myUid();
        return context.checkPermission(permission, pid, uid);
    }

    public static boolean isDenied(Context context, String permission) {
        if (context == null || TextUtils.isEmpty(permission)) {
            return true;
        }
        return checkPermission(context, permission) == PackageManager.PERMISSION_DENIED;
    }

    public static boolean isDenied(Context context, List<String> permissions) {
        if (permissions == null || permissions.size() == 0) {
            return false;
        }
        boolean result = false;
        for (String permission : permissions) {
            result |= isDenied(context, permission);
        }
        return result;
    }

    public static boolean isGranted(Context context, String permission) {
        if (context == null || TextUtils.isEmpty(permission)) {
            return false;
        }
        return checkPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isGranted(Context context, String[] permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        boolean result = true;
        for (String permission : permissions) {
            result &= isGranted(context, permission);
        }
        return result;
    }

    public static boolean isGranted(Context context, List<String> permissions) {
        if (permissions == null || permissions.size() == 0) {
            return false;
        }
        String[] stringArray = new String[permissions.size()];
        return isGranted(context, permissions.toArray(stringArray));
    }

    public static boolean isSupportBle(Context context) {
        if (context == null || context.getApplicationContext() == null) {
            return false;
        }
        PackageManager pm = context.getApplicationContext().getPackageManager();
        return isGreaterOrEqual4_3() && pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static boolean isNeedBluetoothGrant() {
        return GeneralUtil.isGreaterOrEqual12();
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private static List<String> getBluetoothPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        return permissions;
    }

    public static boolean isBluetoothGranted(Context context) {
        //如果系统版本小于Android12.0(S/31)，默认返回已授权。
        return !isNeedBluetoothGrant() || isGranted(context, getBluetoothPermissions());
    }

    public static boolean isBleSupportRequestPriority() {
        return isGreaterThanOrEqual(Build.VERSION_CODES.LOLLIPOP);
    }

    private static List<String> getLocationPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        return permissions;
    }

    public static boolean isNeedLocationGrant() {
        return isGreaterOrEqual6_0();
    }


    public static boolean isLocationGranted(Context context) {
        //如果系统版本小于Android6.0(M/23)，默认返回已授权。
        return !isNeedLocationGrant() || isGranted(context, getLocationPermissions());
    }

    public static boolean isLocationDenied(Context context) {
        //如果系统版本小于Android6.0(M/23)，默认返回未拒绝。
        return isNeedLocationGrant() && isDenied(context, getLocationPermissions());
    }
}
