package cn.bingerz.flipble.central.callback;


import java.util.List;

import cn.bingerz.flipble.central.ScanDevice;

public abstract class ScanCallback {

    public abstract void onScanStarted(boolean success);

    public abstract void onScanning(ScanDevice result);

    public abstract void onScanFinished(List<ScanDevice> scanResultList);
}
