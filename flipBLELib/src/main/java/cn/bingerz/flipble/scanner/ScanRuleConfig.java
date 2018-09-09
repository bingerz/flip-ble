package cn.bingerz.flipble.scanner;


import java.util.ArrayList;
import java.util.List;

import cn.bingerz.flipble.central.CentralManager;

public class ScanRuleConfig {

    public static final int SCAN_MODE_LOW_POWER = 0;
    public static final int SCAN_MODE_BALANCED = 1;
    public static final int SCAN_MODE_HIGH_POWER = 2;

    private int mScanMode;
    private long mScanDuration;
    private List<ScanFilterConfig> mScanFilterConfigs = new ArrayList<>();

    public int getScanMode() {
        return mScanMode;
    }

    public long getScanDuration() {
        return mScanDuration;
    }

    public List<ScanFilterConfig> getScanFilterConfigs() {
        return mScanFilterConfigs;
    }

    public static class Builder {

        private int mScanMode = SCAN_MODE_HIGH_POWER;
        private long mDuration = CentralManager.DEFAULT_SCAN_TIME;
        private List<ScanFilterConfig> mScanFilterConfigs = new ArrayList<>();

        public Builder setScanMode(int scanMode) {
            this.mScanMode = scanMode;
            return this;
        }

        public Builder setScanDuration(long duration) {
            this.mDuration = duration;
            return this;
        }

        public Builder setScanFilterConfigs(List<ScanFilterConfig> scanFilterConfigs) {
            this.mScanFilterConfigs = scanFilterConfigs;
            return this;
        }

        void applyConfig(ScanRuleConfig config) {
            config.mScanMode = this.mScanMode;
            config.mScanDuration = this.mDuration;
            config.mScanFilterConfigs = this.mScanFilterConfigs;
        }

        public ScanRuleConfig build() {
            ScanRuleConfig config = new ScanRuleConfig();
            applyConfig(config);
            return config;
        }
    }
}
