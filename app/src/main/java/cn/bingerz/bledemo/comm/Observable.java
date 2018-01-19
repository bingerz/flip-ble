package cn.bingerz.bledemo.comm;


import cn.bingerz.flipble.peripheral.Peripheral;

public interface Observable {

    void addObserver(Observer obj);

    void deleteObserver(Observer obj);

    void notifyObserver(Peripheral peripheral);
}
