package cn.bingerz.flipble.callback;

import cn.bingerz.flipble.bluetoothle.PeripheralController;
import cn.bingerz.flipble.exception.BleException;

/**
 * Created by hanson on 09/01/2018.
 */

public abstract class RssiCallback {

    public abstract void onRssiFailure(BleException exception);

    public abstract void onRssiSuccess(int rssi);

    private PeripheralController peripheralConnector;

    public void setPeripheralConnector(PeripheralController peripheralConnector) {
        this.peripheralConnector = peripheralConnector;
    }

    public PeripheralController getPeripheralConnector() {
        return peripheralConnector;
    }
}
