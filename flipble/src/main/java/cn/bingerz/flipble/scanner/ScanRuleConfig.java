package cn.bingerz.flipble.scanner;


import java.util.ArrayList;
import java.util.List;

/**
 * @author hanson
 */
public class ScanRuleConfig {

    public static final int SCAN_MODE_LOW_POWER = 0;
    public static final int SCAN_MODE_BALANCED = 1;
    public static final int SCAN_MODE_HIGH_POWER = 2;

    private int mScanMode;
    private long mScanDuration;
    private long mScanInterval;

    private List<ScanFilterConfig> mScanFilterConfigs = new ArrayList<>();

    public int getScanMode() {
        return mScanMode;
    }

    public long getScanDuration() {
        return mScanDuration;
    }

    public long getScanInterval() {
        return mScanInterval;
    }

    public List<ScanFilterConfig> getScanFilterConfigs() {
        return mScanFilterConfigs;
    }

    @Override
    public String toString() {
        return "ScanRuleConfig{" +
                "mScanMode=" + mScanMode +
                ", mScanDuration=" + mScanDuration +
                ", mScanInterval=" + mScanInterval +
                ", mScanFilterConfigs=" + mScanFilterConfigs +
                '}';
    }

    public static class Builder {
        private int mScanMode = SCAN_MODE_HIGH_POWER;
        private long mDuration;
        private long mInterval;
        private List<ScanFilterConfig> mScanFilterConfigs = new ArrayList<>();

        public Builder setScanMode(int scanMode) {
            this.mScanMode = scanMode;
            return this;
        }

        public Builder setScanDuration(long duration) {
            this.mDuration = duration;
            return this;
        }

        public Builder setScanInterval(long interval) {
            this.mInterval = interval;
            return this;
        }

        public Builder setScanFilterConfigs(List<ScanFilterConfig> scanFilterConfigs) {
            this.mScanFilterConfigs = scanFilterConfigs;
            return this;
        }

        void applyConfig(ScanRuleConfig config) {
            config.mScanMode = this.mScanMode;
            config.mScanDuration = this.mDuration;
            config.mScanInterval = this.mInterval;
            config.mScanFilterConfigs = this.mScanFilterConfigs;
        }

        public ScanRuleConfig build() {
            ScanRuleConfig config = new ScanRuleConfig();
            applyConfig(config);
            return config;
        }
    }
}
