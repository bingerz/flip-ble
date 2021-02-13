package cn.bingerz.flipble.peripheral;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.peripheral.command.Command;
import cn.bingerz.flipble.peripheral.command.CommandStack;
import cn.bingerz.flipble.utils.BleLruHashMap;
import cn.bingerz.flipble.utils.EasyLog;

/**
 * @author hanson
 */
public class MultiplePeripheralController {

    private final BleLruHashMap<String, Peripheral> bleLruHashMap;

    private CommandStack mCommandStack;

    public MultiplePeripheralController() {
        bleLruHashMap = new BleLruHashMap<>(CentralManager.getInstance().getMaxConnectCount());
        mCommandStack = new CommandStack();
    }

    public void addPeripheral(Peripheral peripheral) {
        if (peripheral == null) {
            return;
        }
        bleLruHashMap.put(peripheral.getAddress(), peripheral);
    }

    public void removePeripheral(Peripheral peripheral) {
        if (peripheral == null) {
            return;
        }
        bleLruHashMap.remove(peripheral.getAddress());
    }

    public boolean isContainDevice(String key) {
        return !TextUtils.isEmpty(key) && bleLruHashMap.containsKey(key);
    }

    public Peripheral getPeripheral(String key) {
        if (!TextUtils.isEmpty(key)) {
            if (bleLruHashMap.containsKey(key)) {
                return bleLruHashMap.get(key);
            }
        }
        return null;
    }

    public boolean isContainBusyDevice() {
        boolean isBusy = false;
        for (Map.Entry<String, Peripheral> stringPeripheralEntry : bleLruHashMap.entrySet()) {
            if (stringPeripheralEntry.getValue().isBusyState()) {
                isBusy = true;
                break;
            }
        }
        return isBusy;
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

    public void cacheCommand(Command command) {
        if (mCommandStack != null) {
            EasyLog.d("Peripherals is busy, cache cmd=%s", command);
            mCommandStack.add(command);
        }
    }

    public void removeCommand(Command command) {
        if (mCommandStack != null) {
            EasyLog.d("Peripherals is busy, remove cmd=%s", command);
            mCommandStack.remove(command);
        }
    }

    public void printCommandQueue() {
        if (mCommandStack != null) {
            mCommandStack.printQueue();
        }
    }

    public void executeNextCommand() {
        if (!isContainBusyDevice() && mCommandStack != null) {
            Command command = mCommandStack.poll();
            if (command != null && command.isValid()) {
                EasyLog.d("Peripherals is idle, execute next command=%s", command);
                if (CentralManager.getInstance().isBLEConnected(command.getKey())) {
                    Peripheral peripheral = getPeripheral(command.getKey());
                    if (peripheral != null) {
                        switch (command.getMethod()) {
                            case Command.Method.NOTIFY:
                                peripheral.notify(command);
                                break;
                            case Command.Method.INDICATE:
                                peripheral.indicate(command);
                                break;
                            case Command.Method.READ:
                                peripheral.read(command);
                                break;
                            case Command.Method.WRITE:
                                peripheral.write(command);
                                break;
                            case Command.Method.READ_RSSI:
                                peripheral.readRssi(command);
                                break;
                            case Command.Method.SET_MTU:
                                peripheral.setMtu(command);
                                break;
                            default:
                                break;
                        }
                    }
                } else {
                    EasyLog.w("Peripheral is disconnect, Throw away the command.");
                    executeNextCommand();
                }
            }
        }
    }
}
