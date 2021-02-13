package cn.bingerz.flipble.peripheral;

/**
 * @author hanson
 */
public enum ConnectionState {

    CONNECT_IDLE(0x00),
    CONNECT_CONNECTING(0x01),
    CONNECT_CONNECTED(0x02),
    CONNECT_FAILURE(0x03),
    CONNECT_TIMEOUT(0x04),
    CONNECT_DISCONNECTING(0x05),
    CONNECT_DISCONNECTED(0x06);

    private int code;

    ConnectionState(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
