package cn.bingerz.flipble.central.callback;


import java.util.List;

import cn.bingerz.flipble.peripheral.Peripheral;

public abstract class ScanCallback {

    public abstract void onScanStarted(boolean success);

    public abstract void onScanning(Peripheral result);

    public abstract void onScanFinished(List<Peripheral> scanResultList);

    public void onLeScan(Peripheral peripheral){}
}
