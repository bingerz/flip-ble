package cn.bingerz.flipble.callback;

import cn.bingerz.flipble.bluetoothle.PeripheralController;
import cn.bingerz.flipble.exception.BleException;

/**
 * Created by hanson on 09/01/2018.
 */

public abstract class WriteCallback {

    public abstract void onWriteSuccess();

    public abstract void onWriteFailure(BleException exception);

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
