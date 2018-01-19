package cn.bingerz.bledemo.adapter;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.bingerz.bledemo.R;
import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.peripheral.Peripheral;

public class PeripheralAdapter extends BaseAdapter {

    private Context context;
    private List<Peripheral> peripheralList;

    public PeripheralAdapter(Context context) {
        this.context = context;
        peripheralList = new ArrayList<>();
    }

    public void addDevice(Peripheral peripheral) {
        removeDevice(peripheral);
        peripheralList.add(peripheral);
    }

    public void removeDevice(Peripheral peripheral) {
        for (int i = 0; i < peripheralList.size(); i++) {
            Peripheral device = peripheralList.get(i);
            if (peripheral.getKey().equals(device.getKey())) {
                peripheralList.remove(i);
            }
        }
    }

    public void clearConnectedDevice() {
        for (int i = 0; i < peripheralList.size(); i++) {
            Peripheral peripheral = peripheralList.get(i);
            if (CentralManager.getInstance().isConnected(peripheral.getKey())) {
                peripheralList.remove(i);
            }
        }
    }

    public void clearScanDevice() {
        for (int i = 0; i < peripheralList.size(); i++) {
            Peripheral peripheral = peripheralList.get(i);
            if (!CentralManager.getInstance().isConnected(peripheral.getKey())) {
                peripheralList.remove(i);
            }
        }
    }

    public void clear() {
        clearConnectedDevice();
        clearScanDevice();
    }

    @Override
    public int getCount() {
        return peripheralList.size();
    }

    @Override
    public Peripheral getItem(int position) {
        if (position > peripheralList.size())
            return null;
        return peripheralList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, final ViewGroup parent) {
        ViewHolder holder;
        if (convertView != null) {
            holder = (ViewHolder) convertView.getTag();
        } else {
            convertView = View.inflate(context, R.layout.adapter_device, null);
            holder = new ViewHolder();
            convertView.setTag(holder);
            holder.img_blue = convertView.findViewById(R.id.img_blue);
            holder.txt_name = convertView.findViewById(R.id.txt_name);
            holder.txt_mac = convertView.findViewById(R.id.txt_mac);
            holder.txt_rssi = convertView.findViewById(R.id.txt_rssi);
            holder.layout_idle = convertView.findViewById(R.id.layout_idle);
            holder.layout_connected = convertView.findViewById(R.id.layout_connected);
            holder.btn_disconnect = convertView.findViewById(R.id.btn_disconnect);
            holder.btn_connect = convertView.findViewById(R.id.btn_connect);
            holder.btn_detail = convertView.findViewById(R.id.btn_detail);
        }

        final Peripheral peripheral = getItem(position);
        if (peripheral != null) {
            boolean isConnected = CentralManager.getInstance().isConnected(peripheral.getKey());
            String name = peripheral.getName();
            String mac = peripheral.getMac();
            int rssi = peripheral.getRssi();
            holder.txt_name.setText(name);
            holder.txt_mac.setText(mac);
            holder.txt_rssi.setText(String.valueOf(rssi));
            if (isConnected) {
                holder.img_blue.setImageResource(R.mipmap.ic_blue_connected);
                holder.txt_name.setTextColor(0xFF4a90e2);
                holder.txt_mac.setTextColor(0xFF4a90e2);
                holder.layout_idle.setVisibility(View.GONE);
                holder.layout_connected.setVisibility(View.VISIBLE);
            } else {
                holder.img_blue.setImageResource(R.mipmap.ic_blue_remote);
                holder.txt_name.setTextColor(0xFF000000);
                holder.txt_mac.setTextColor(0xFF000000);
                holder.layout_idle.setVisibility(View.VISIBLE);
                holder.layout_connected.setVisibility(View.GONE);
            }
        }

        holder.btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onConnect(peripheral);
                }
            }
        });

        holder.btn_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onDisConnect(peripheral);
                }
            }
        });

        holder.btn_detail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    mListener.onDetail(peripheral);
                }
            }
        });

        return convertView;
    }

    class ViewHolder {
        ImageView img_blue;
        TextView txt_name;
        TextView txt_mac;
        TextView txt_rssi;
        LinearLayout layout_idle;
        LinearLayout layout_connected;
        Button btn_disconnect;
        Button btn_connect;
        Button btn_detail;
    }

    public interface OnDeviceClickListener {
        void onConnect(Peripheral peripheral);

        void onDisConnect(Peripheral peripheral);

        void onDetail(Peripheral peripheral);
    }

    private OnDeviceClickListener mListener;

    public void setOnDeviceClickListener(OnDeviceClickListener listener) {
        this.mListener = listener;
    }

}
