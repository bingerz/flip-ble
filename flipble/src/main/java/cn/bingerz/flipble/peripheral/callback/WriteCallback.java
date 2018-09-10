package cn.bingerz.flipble.peripheral.callback;

import cn.bingerz.flipble.exception.BLEException;
import cn.bingerz.flipble.peripheral.PeripheralController;

/**
 * Created by hanson on 09/01/2018.
 */

public abstract class WriteCallback {

    public abstract void onWriteSuccess();

    public abstract void onWriteFailure(BLEException exception);

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
