package cn.bingerz.flipble.peripheral.callback;

import cn.bingerz.flipble.peripheral.PeripheralController;
import cn.bingerz.flipble.exception.BLEException;

/**
 * @author hanson
 */
public abstract class IndicateCallback {

    public abstract void onIndicateSuccess();

    public abstract void onIndicateFailure(BLEException exception);

    public abstract void onCharacteristicChanged(byte[] data);

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
