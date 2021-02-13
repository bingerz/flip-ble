package cn.bingerz.flipble.peripheral.callback;

import cn.bingerz.flipble.peripheral.PeripheralController;
import cn.bingerz.flipble.exception.BLEException;

/**
 * @author hanson
 */
public abstract class ReadCallback {

    public abstract void onReadSuccess(byte[] data);

    public abstract void onReadFailure(BLEException exception);

    private String key;

    private PeripheralController peripheralConnector;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setPeripheralConnector(PeripheralController peripheralConnector) {
        this.peripheralConnector = peripheralConnector;
    }

    public PeripheralController getPeripheralConnector() {
        return peripheralConnector;
    }
}
