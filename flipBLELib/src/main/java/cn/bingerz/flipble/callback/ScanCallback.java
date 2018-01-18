package cn.bingerz.flipble.callback;


import java.util.List;

import cn.bingerz.flipble.bluetoothle.Peripheral;

public abstract class ScanCallback {

    public abstract void onScanStarted(boolean success);

    public abstract void onScanning(Peripheral result);

    public abstract void onScanFinished(List<Peripheral> scanResultList);

    public void onLeScan(Peripheral peripheral){}
}
