package cn.bingerz.bledemo.comm;


import cn.bingerz.flipble.bluetoothle.Peripheral;

public interface Observer {

    void disConnected(Peripheral peripheral);
}
