package cn.bingerz.flipble.scan;



public enum CentralScanState {

    STATE_IDLE(-1),
    STATE_SCANNING(0X01);

    private int code;

    CentralScanState(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
