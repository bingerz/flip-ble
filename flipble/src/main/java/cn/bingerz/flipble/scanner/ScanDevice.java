package cn.bingerz.flipble.scanner;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author hanson
 */
public class ScanDevice implements Parcelable{

    private int mRssi;
    private byte[] mScanRecord;
    private BluetoothDevice mDevice;

    public ScanDevice(BluetoothDevice device, int rssi, byte[] scanRecord) {
        mDevice = device;
        mScanRecord = scanRecord;
        mRssi = rssi;
    }

    public BluetoothDevice getBluetoothDevice() {
        return mDevice;
    }

    public void setDevice(BluetoothDevice device) {
        this.mDevice = device;
    }

    @SuppressWarnings({"MissingPermission"})
    public String getName() {
        return mDevice != null ? mDevice.getName() : null;
    }

    public String getAddress() {
        return mDevice != null ? mDevice.getAddress() : null;
    }

    public byte[] getScanRecordRaw() {
        return mScanRecord;
    }

    public ScanRecord getScanRecord() {
        return ScanRecord.parseFromBytes(mScanRecord);
    }

    public void setScanRecord(byte[] scanRecord) {
        this.mScanRecord = scanRecord;
    }

    public int getRssi() {
        return mRssi;
    }

    public void setRssi(int rssi) {
        this.mRssi = rssi;
    }

    protected ScanDevice(Parcel in) {
        mDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        mScanRecord = in.createByteArray();
        mRssi = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(mDevice, flags);
        dest.writeByteArray(mScanRecord);
        dest.writeInt(mRssi);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<ScanDevice> CREATOR = new Parcelable.Creator<ScanDevice>() {
        @Override
        public ScanDevice createFromParcel(Parcel in) {
            return new ScanDevice(in);
        }

        @Override
        public ScanDevice[] newArray(int size) {
            return new ScanDevice[size];
        }
    };
}
