package cn.bingerz.flipble.peripheral.callback;

import cn.bingerz.flipble.peripheral.PeripheralController;
import cn.bingerz.flipble.exception.BLEException;

/**
 * @author hanson
 */
public abstract class MtuChangedCallback {

    public abstract void onSetMTUFailure(BLEException exception);

    public abstract void onMtuChanged(int mtu);

    private PeripheralController peripheralConnector;

    public void setPeripheralConnector(PeripheralController peripheralConnector) {
        this.peripheralConnector = peripheralConnector;
    }

    public PeripheralController getPeripheralConnector() {
        return peripheralConnector;
    }
}
