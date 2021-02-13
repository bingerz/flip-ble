package cn.bingerz.flipble.scanner.callback;


import java.util.List;

import cn.bingerz.flipble.scanner.ScanDevice;

/**
 * @author hanson
 */
public abstract class ScanCallback {

    public abstract void onScanStarted();

    public abstract void onScanning(ScanDevice result);

    public abstract void onScanFinished(List<ScanDevice> scanResultList);

    public abstract void onScanFailed(int errorCode);
}
