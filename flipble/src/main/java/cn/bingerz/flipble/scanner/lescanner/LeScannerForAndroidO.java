package cn.bingerz.flipble.scanner.lescanner;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.os.Build;

import cn.bingerz.flipble.scanner.ScanRuleConfig;

/**
 * @author hanson
 */
@TargetApi(Build.VERSION_CODES.O)
public class LeScannerForAndroidO extends LeScannerForLollipop {

    public LeScannerForAndroidO(BluetoothAdapter bluetoothAdapter, ScanRuleConfig config, LeScanCallback callback) {
        super(bluetoothAdapter, config, callback);
    }
}
