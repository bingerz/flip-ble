package cn.bingerz.flipble.peripheral;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.utils.BleLruHashMap;

/**
 * Created by hanson on 09/01/2018.
 */

public class MultiplePeripheralController {

    private final BleLruHashMap<String, Peripheral> bleLruHashMap;

    public MultiplePeripheralController() {
        bleLruHashMap = new BleLruHashMap<>(CentralManager.getInstance().getMaxConnectCount());
    }

    public void addPeripheral(Peripheral peripheral) {
        if (peripheral == null) {
            return;
        }
        if (!bleLruHashMap.containsKey(peripheral.getAddress())) {
            bleLruHashMap.put(peripheral.getAddress(), peripheral);
        }
    }

    public void removePeripheral(Peripheral peripheral) {
        if (peripheral == null) {
            return;
        }
        bleLruHashMap.remove(peripheral.getAddress());
    }

    public boolean isContainDevice(String key) {
        if (TextUtils.isEmpty(key) || !bleLruHashMap.containsKey(key)) {
            return false;
        }
        return true;
    }

    public Peripheral getPeripheral(String key) {
        if (!TextUtils.isEmpty(key)) {
            if (bleLruHashMap.containsKey(key)) {
                return bleLruHashMap.get(key);
            }
        }
        return null;
    }

    public void disconnectAllDevice() {
        for (Map.Entry<String, Peripheral> stringPeripheralEntry : bleLruHashMap.entrySet()) {
            stringPeripheralEntry.getValue().disconnect();
        }
        bleLruHashMap.clear();
    }

    public void destroy() {
        for (Map.Entry<String, Peripheral> stringPeripheralEntry : bleLruHashMap.entrySet()) {
            stringPeripheralEntry.getValue().destroy();
        }
        bleLruHashMap.clear();
    }

    public List<Peripheral> getPeripheralList() {
        final List<Peripheral> bleBluetoothList = new ArrayList<>(bleLruHashMap.values());
        Collections.sort(bleBluetoothList, new Comparator<Peripheral>() {
            @Override
            public int compare(final Peripheral lhs, final Peripheral rhs) {
                return lhs.getAddress().compareToIgnoreCase(rhs.getAddress());
            }
        });
        return bleBluetoothList;
    }
}
