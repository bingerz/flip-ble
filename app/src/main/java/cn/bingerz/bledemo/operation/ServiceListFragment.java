package cn.bingerz.bledemo.operation;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import cn.bingerz.bledemo.R;
import cn.bingerz.bledemo.util.BleUtils;
import cn.bingerz.flipble.peripheral.Peripheral;
import cn.bingerz.flipble.scanner.ScanRecord;
import cn.bingerz.flipble.utils.BluetoothGattCompat;

/**
 * @author hanson
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ServiceListFragment extends Fragment {

    private static final String TAG = ServiceListFragment.class.getSimpleName();

    private TextView tvName, tvMacAddress, tvExtra;
    private ResultAdapter mResultAdapter;
    private String extraStr;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_service_list, null);
        initView(v);
        showDeviceData();
        return v;
    }

    private void initView(View v) {
        tvName = (TextView) v.findViewById(R.id.tv_service_list_name);
        tvMacAddress = (TextView) v.findViewById(R.id.tv_service_list_mac);
        tvExtra = (TextView) v.findViewById(R.id.tv_service_list_extra);

        mResultAdapter = new ResultAdapter();
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.rv_service_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(mResultAdapter);
    }

    private void showDeviceData() {
        Peripheral peripheral = ((OperationActivity) getActivity()).getPeripheral();
        String name = peripheral.getName();
        String mac = peripheral.getAddress();
        BluetoothGattCompat gattCompat = peripheral.getBluetoothGattCompat();

        tvName.setText(String.valueOf(getActivity().getString(R.string.name) + name));
        tvMacAddress.setText(String.valueOf(getActivity().getString(R.string.mac) + mac));
        extraStr = getFormattedAdv(peripheral);
        tvExtra.setText(extraStr);

        mResultAdapter.clear();
        for (BluetoothGattService service : gattCompat.getServices()) {
            mResultAdapter.addResult(service);
        }
        mResultAdapter.notifyDataSetChanged();
    }

    private void updateExtra(String extra) {
        if (!TextUtils.isEmpty(extraStr)) {
            extraStr = extraStr + "\n";
        }
        extraStr = extraStr + extra;
        tvExtra.setText(extraStr);
    }

    private String getFormattedAdv(Peripheral peripheral) {
        StringBuffer sb = new StringBuffer();
        if (peripheral != null && peripheral.getDevice() != null && peripheral.getDevice().getScanRecord() != null) {
            ScanRecord scanRecord = peripheral.getDevice().getScanRecord();

            //Parsing the ServiceData UUID/Data in the Bluetooth broadcast
            String serviceDataUUID = BleUtils.parseAdvServiceDataUUID(scanRecord);
            String serviceData = BleUtils.parseAdvServiceData(scanRecord);
            //Parsing the ManufacturerSpecific Key/Data in the Bluetooth broadcast
            String manufacturerKey = BleUtils.parseAdvManufacturerDataKey(scanRecord);
            String manufacturerData = BleUtils.parseAdvManufacturerData(scanRecord);

            //Protocol Type
            String advType = BleUtils.parseAdvType(scanRecord);
            if (!TextUtils.isEmpty(advType)) {
                sb.append("AdvType:");
                sb.append(advType);
                sb.append("\n");
            }

            if (!TextUtils.isEmpty(serviceDataUUID)) {
                sb.append("ServiceData UUID:");
                sb.append(serviceDataUUID);
                sb.append(" Data:");
                sb.append(serviceData);
                sb.append("\n");
            }
            if (!TextUtils.isEmpty(manufacturerKey)) {
                sb.append("ManufacturerData Key:");
                sb.append(manufacturerKey);
                sb.append(" Data:");
                sb.append(manufacturerData);
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {

        private List<BluetoothGattService> mBluetoothGattServices;

        ResultAdapter() {
            mBluetoothGattServices = new ArrayList<>();
        }

        void addResult(BluetoothGattService service) {
            mBluetoothGattServices.add(service);
        }

        void clear() {
            mBluetoothGattServices.clear();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_service, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final BluetoothGattService service = mBluetoothGattServices.get(position);
            String uuid = service.getUuid().toString();

            holder.tvTitle.setText(String.valueOf(getActivity().getString(R.string.service) + "(" + position + ")"));
            holder.tvUUID.setText(uuid);
            holder.tvType.setText(getActivity().getString(R.string.type));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((OperationActivity) getActivity()).setBluetoothGattService(service);
                    ((OperationActivity) getActivity()).changePage(1);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mBluetoothGattServices == null ? 0 : mBluetoothGattServices.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvUUID;
            TextView tvType;

            public ViewHolder(View itemView) {
                super(itemView);
                tvTitle = (TextView) itemView.findViewById(R.id.tv_title);
                tvUUID = (TextView) itemView.findViewById(R.id.tv_uuid);
                tvType = (TextView) itemView.findViewById(R.id.tv_type);
            }
        }
    }
}
