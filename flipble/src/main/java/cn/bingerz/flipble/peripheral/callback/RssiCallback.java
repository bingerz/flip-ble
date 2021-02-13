package cn.bingerz.flipble.peripheral.callback;

import cn.bingerz.flipble.peripheral.PeripheralController;
import cn.bingerz.flipble.exception.BLEException;

/**
 * @author hanson
 */
public abstract class RssiCallback {

    public abstract void onRssiFailure(BLEException exception);

    public abstract void onRssiSuccess(int rssi);

    private PeripheralController peripheralConnector;

    public void setPeripheralConnector(PeripheralController peripheralConnector) {
        this.peripheralConnector = peripheralConnector;
    }

    public PeripheralController getPeripheralConnector() {
        return peripheralConnector;
    }
}
