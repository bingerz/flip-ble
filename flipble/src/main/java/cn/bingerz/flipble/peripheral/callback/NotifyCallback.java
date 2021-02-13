package cn.bingerz.flipble.peripheral.callback;

import cn.bingerz.flipble.exception.BLEException;
import cn.bingerz.flipble.peripheral.PeripheralController;

/**
 * @author hanson
 */
public abstract class NotifyCallback {

    public abstract void onNotifySuccess();

    public abstract void onNotifyFailure(BLEException exception);

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
