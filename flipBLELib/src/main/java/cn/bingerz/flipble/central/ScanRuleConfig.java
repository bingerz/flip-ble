package cn.bingerz.flipble.central;


import java.util.UUID;

public class ScanRuleConfig {

    private UUID[] mServiceUUIDs = null;
    private String[] mDeviceNames = null;
    private String mDeviceMac = null;
//    private boolean mAutoConnect = false;
    private boolean mFuzzy = false;
    private long mScanTimeOut = CentralManager.DEFAULT_SCAN_TIME;

    public UUID[] getServiceUUIDs() {
        return mServiceUUIDs;
    }

    public String[] getDeviceNames() {
        return mDeviceNames;
    }

    public String getDeviceMac() {
        return mDeviceMac;
    }

    public boolean isFuzzy() {
        return mFuzzy;
    }

    public long getScanTimeOut() {
        return mScanTimeOut;
    }

    public static class Builder {

        private UUID[] mServiceUuids = null;
        private String[] mDeviceNames = null;
        private String mDeviceMac = null;
        private boolean mAutoConnect = false;
        private boolean mFuzzy = false;
        private long mTimeOut = CentralManager.DEFAULT_SCAN_TIME;

        public Builder setServiceUuids(UUID[] uuids) {
            this.mServiceUuids = uuids;
            return this;
        }

        public Builder setDeviceName(boolean fuzzy, String... name) {
            this.mFuzzy = fuzzy;
            this.mDeviceNames = name;
            return this;
        }

        public Builder setDeviceMac(String mac) {
            this.mDeviceMac = mac;
            return this;
        }

        public Builder setScanTimeOut(long timeOut) {
            this.mTimeOut = timeOut;
            return this;
        }

        void applyConfig(ScanRuleConfig config) {
            config.mServiceUUIDs = this.mServiceUuids;
            config.mDeviceNames = this.mDeviceNames;
            config.mDeviceMac = this.mDeviceMac;
            config.mFuzzy = this.mFuzzy;
            config.mScanTimeOut = this.mTimeOut;
        }

        public ScanRuleConfig build() {
            ScanRuleConfig config = new ScanRuleConfig();
            applyConfig(config);
            return config;
        }

    }


}
