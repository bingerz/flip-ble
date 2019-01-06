package cn.bingerz.flipble.scanner.lescanner;


public enum LeScanState {

    STATE_IDLE(-1),
    STATE_SCANNING(0x01);

    private int code;

    LeScanState(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
