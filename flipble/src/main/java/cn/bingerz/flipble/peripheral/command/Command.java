package cn.bingerz.flipble.peripheral.command;

import java.util.Arrays;
import java.util.Comparator;

/**
 * @author hanson
 */
public class Command implements Comparator<Command> {

    /** Supported command methods. */
    public interface Priority {
        int HIGH = 0;
        int MEDIUM = 1;
        int LOW = 2;
    }

    /** Supported command methods. */
    public interface Method {
        int NOTIFY = 0;
        int INDICATE = 1;
        int READ = 2;
        int WRITE = 3;
        int READ_RSSI = 4;
        int SET_MTU = 5;
    }

    public static byte[] ENABLE = {0x1};
    public static byte[] DISABLE = {0x0};

    /*
    Priority of Bluetooth command execution
     */
    private int priority;

    private String key;

    private int method;

    private String serviceUUID;

    private String charactUUID;

    private byte[] data;

    private Object callback;

    public Command(int priority, String key, int method, Object callback) {
        this(priority, key, method, null, callback);
    }

    public Command(int priority, String key, int method, byte[] data, Object callback) {
        this(priority, key, method, null, null, data, callback);
    }

    public Command(int priority, String key, int method, String serviceUUID, String charactUUID, Object callback) {
        this(priority, key, method, serviceUUID, charactUUID, null, callback);
    }

    public Command(int priority, String key, int method, String serviceUUID, String charactUUID, byte[] data, Object callback) {
        setPriority(priority);
        setKey(key);
        setMethod(method);
        setServiceUUID(serviceUUID);
        setCharactUUID(charactUUID);
        setData(data);
        setCallback(callback);
    }

    /** Return the priority for this command. Can be one of the values in {@link Priority}. */
    public int getPriority() {
        return this.priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    /** Return the key of peripheral for this command.*/
    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /** Return the method for this command. Can be one of the values in {@link Method}. */
    public int getMethod() {
        return method;
    }

    public void setMethod(int method) {
        this.method = method;
    }

    public String getServiceUUID() {
        return this.serviceUUID;
    }

    public void setServiceUUID(String uuid) {
        this.serviceUUID = uuid;
    }

    public String getCharactUUID() {
        return this.charactUUID;
    }

    public void setCharactUUID(String uuid) {
        this.charactUUID = uuid;
    }

    public void setData(byte[] value) {
        if (value != null && value.length > 0) {
            data = new byte[value.length];
            System.arraycopy(value, 0, data, 0, value.length);
        }
    }

    public byte[] getData() {
        return data;
    }

    public boolean isEnable() {
        if (data != null && data.length >= 1) {
            return this.data[0] == 1;
        }
        return false;
    }

    public void setCallback(Object callback) {
        this.callback = callback;
    }

    public Object getCallback() {
        return this.callback;
    }

    public boolean isValid() {
        return (key != null && key.length() > 0)
                && method >= Method.NOTIFY;
    }

    @Override
    public int compare(Command o1, Command o2) {
        if (o1.priority != o2.priority) {
            return o1.priority - o2.priority;
        } else {
            if (o1.method != o2.method) {
                return o1.method - o2.method;
            } else {
                if (!o1.key.equals(o2.key)) {
                    return o1.key.compareTo(o2.key);
                } else {
                    return -1;
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Command{" +
                "priority=" + priority +
                ", key='" + key + '\'' +
                ", method=" + method +
                ", serviceUUID='" + serviceUUID + '\'' +
                ", charactUUID='" + charactUUID + '\'' +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}
