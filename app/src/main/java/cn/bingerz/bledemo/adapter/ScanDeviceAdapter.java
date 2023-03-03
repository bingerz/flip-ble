package cn.bingerz.bledemo.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.bingerz.bledemo.R;
import cn.bingerz.bledemo.util.BleUtils;
import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.scanner.ScanDevice;
import cn.bingerz.flipble.scanner.ScanRecord;

/**
 * @author hanson
 */
public class
ScanDeviceAdapter extends RecyclerView.Adapter<ScanDeviceAdapter.ViewHolder> {

    private List<ScanDevice> mScanDevices;
    private Map<String, Long> mAdvIntervalMap = new HashMap<>();

    public ScanDeviceAdapter() {
        mScanDevices = new ArrayList<>();
    }

    public int insertDevice(ScanDevice device) {
        int index = findDevice(device.getAddress());
        if (index >= 0) {
            ScanDevice latest = mScanDevices.get(index);
            long interval = device.getCreateTime() - latest.getCreateTime();
            mAdvIntervalMap.put(device.getAddress(), interval);
            updateDevice(index, device);
        } else {
            mScanDevices.add(device);
        }
        return index;
    }

    public void removeDevice(String address) {
        for (int i = 0; i < mScanDevices.size(); i++) {
            ScanDevice device = mScanDevices.get(i);
            if (device.getAddress().equals(address)) {
                mScanDevices.remove(i);
            }
        }
    }

    private void updateDevice(int index, ScanDevice device) {
        if (index >= 0 && index < mScanDevices.size() && device != null) {
            ScanDevice cur = mScanDevices.get(index);
            if (cur != null && cur.getAddress().equals(device.getAddress())) {
                mScanDevices.set(index, device);
            }
        }
    }

    private int findDevice(String address) {
        int index = -1;
        for (int i = 0; i < mScanDevices.size(); i++) {
            ScanDevice device = mScanDevices.get(i);
            if (device.getAddress().equals(address)) {
                index = i;
            }
        }
        return index;
    }

    public void sortDevice() {
        Collections.sort(mScanDevices, (d1, d2) -> {
            int diff = d1.getRssi() - d2.getRssi();
            if (diff > 0) {
                return -1;
            } else if (diff < 0) {
                return 1;
            }
            return 0; //相等为0
        });
    }

    public void updateConnectedDevice(ScanDevice device) {
        removeDevice(device.getAddress());
        mScanDevices.add(0, device);
    }

    public void clearConnectedDevice() {
        Iterator<ScanDevice> it = mScanDevices.iterator();
        while (it.hasNext()) {
            ScanDevice device = it.next();
            if (CentralManager.getInstance().isConnected(device.getAddress())) {
                it.remove();
            }
        }
    }

    public void clearScanDevice() {
        Iterator<ScanDevice> it = mScanDevices.iterator();
        while (it.hasNext()) {
            ScanDevice device = it.next();
            if (!CentralManager.getInstance().isConnected(device.getAddress())) {
                it.remove();
            }
        }
    }

    private long getAdvInterval(String address) {
        long advInterval = -1;
        if (mAdvIntervalMap != null && mAdvIntervalMap.containsKey(address) && mAdvIntervalMap.get(address) != null) {
            advInterval = mAdvIntervalMap.get(address);
        }
        return advInterval;
    }

    private void clearAdvIntervalMap() {
        if (mAdvIntervalMap != null && !mAdvIntervalMap.isEmpty()) {
            mAdvIntervalMap.clear();
        }
    }

    public void clear() {
        clearConnectedDevice();
        clearScanDevice();
        clearAdvIntervalMap();
    }

    @Override
    public int getItemCount() {
        return mScanDevices == null ? 0 : mScanDevices.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_device, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final ScanDevice device = mScanDevices.get(position);
        if (device != null) {
            boolean isConnected = CentralManager.getInstance().isConnected(device.getAddress());
            String defaultNullName = "Null";
            String name = device.getName();
            if (TextUtils.isEmpty(name)) {
                name = defaultNullName;
            }
            String mac = device.getAddress();
            int rssi = device.getRssi();
            holder.tvName.setText(name);
            holder.tvMacAddress.setText(mac);
            long advInterval = getAdvInterval(device.getAddress());
            if (advInterval > 1000) {
                holder.tvAdvInterval.setText(String.format(Locale.getDefault(), "%d.%ds", advInterval / 1000, advInterval % 1000));
            } else if (advInterval > 0) {
                holder.tvAdvInterval.setText(String.format(Locale.getDefault(), "%sms", advInterval));
            } else {
                holder.tvAdvInterval.setText("");
            }
            holder.tvRssi.setText(String.valueOf(rssi));

            ScanRecord scanRecord = device.getScanRecord();
            if (scanRecord != null) {
                //Parsing the ServiceData UUID in the Bluetooth broadcast
                String serviceDataUUID = BleUtils.parseAdvServiceDataUUID(scanRecord);
                //Parsing the ManufacturerSpecificData Key in the Bluetooth broadcast
                String manufacturerKey = BleUtils.parseAdvManufacturerDataKey(scanRecord);

                StringBuffer sb = new StringBuffer();
                if (!TextUtils.isEmpty(manufacturerKey)) {
                    sb.append("MD:");
                    sb.append(manufacturerKey);
                }
                if (!TextUtils.isEmpty(serviceDataUUID)) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append("SD:");
                    sb.append(serviceDataUUID);
                }
                String extraData = sb.toString();
                if (!TextUtils.isEmpty(extraData)) {
                    holder.tvExtraData.setText(String.format(Locale.getDefault(),"%s", extraData));
                }
                String advType = BleUtils.parseAdvType(scanRecord);
                if (name.equals(defaultNullName) && !TextUtils.isEmpty(advType)) {
                    holder.tvName.setText(advType);
                }
            }
            if (isConnected) {
                holder.ivBluetooth.setImageResource(R.mipmap.ic_blue_connected);
                holder.tvName.setTextColor(0xFF4a90e2);
                holder.tvExtraData.setTextColor(0xFF4a90e2);
                holder.tvMacAddress.setTextColor(0xFF4a90e2);
                holder.llIdle.setVisibility(View.GONE);
                holder.llConnected.setVisibility(View.VISIBLE);
            } else {
                holder.ivBluetooth.setImageResource(R.mipmap.ic_blue_remote);
                holder.tvName.setTextColor(0xFF000000);
                holder.tvExtraData.setTextColor(0xFF000000);
                holder.tvMacAddress.setTextColor(0xFF000000);
                holder.llIdle.setVisibility(View.VISIBLE);
                holder.llConnected.setVisibility(View.GONE);
            }
        }
        holder.btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onConnect(device);
                }
            }
        });

        holder.btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onDisConnect(device);
                }
            }
        });

        holder.btnDetail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onDetail(device);
                }
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivBluetooth;
        private TextView tvName;
        private TextView tvExtraData;
        private TextView tvMacAddress;
        private TextView tvAdvInterval;
        private TextView tvRssi;
        private LinearLayout llIdle;
        private LinearLayout llConnected;
        private Button btnDisconnect;
        private Button btnConnect;
        private Button btnDetail;

        public ViewHolder(View itemView) {
            super(itemView);
            ivBluetooth = itemView.findViewById(R.id.iv_bluetooth);
            tvName = itemView.findViewById(R.id.tv_name);
            tvExtraData = itemView.findViewById(R.id.tv_extra_data);
            tvMacAddress = itemView.findViewById(R.id.tv_mac);
            tvAdvInterval = itemView.findViewById(R.id.tv_adv_interval);
            tvRssi = itemView.findViewById(R.id.tv_rssi);
            llIdle = itemView.findViewById(R.id.layout_idle);
            llConnected = itemView.findViewById(R.id.layout_connected);
            btnDisconnect = itemView.findViewById(R.id.btn_disconnect);
            btnConnect = itemView.findViewById(R.id.btn_connect);
            btnDetail = itemView.findViewById(R.id.btn_detail);
        }
    }

    public interface OnDeviceClickListener {
        void onConnect(ScanDevice device);

        void onDisConnect(ScanDevice device);

        void onDetail(ScanDevice device);
    }

    private OnDeviceClickListener mListener;

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.mListener = listener;
    }

}
