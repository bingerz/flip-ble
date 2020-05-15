package cn.bingerz.bledemo.operation;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

import cn.bingerz.bledemo.R;
import cn.bingerz.flipble.peripheral.Peripheral;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ServiceListFragment extends Fragment {

    private TextView tvName, tvMacAddress;
    private ResultAdapter mResultAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_service_list, null);
        initView(v);
        showData();
        return v;
    }

    private void initView(View v) {
        tvName = (TextView) v.findViewById(R.id.tv_service_list_name);
        tvMacAddress = (TextView) v.findViewById(R.id.tv_service_list_mac);

        mResultAdapter = new ResultAdapter();
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.rv_service_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(mResultAdapter);
    }

    private void showData() {
        Peripheral peripheral = ((OperationActivity) getActivity()).getPeripheral();
        String name = peripheral.getName();
        String mac = peripheral.getAddress();
        BluetoothGatt gatt = peripheral.getBluetoothGatt();

        tvName.setText(String.valueOf(getActivity().getString(R.string.name) + name));
        tvMacAddress.setText(String.valueOf(getActivity().getString(R.string.mac) + mac));

        mResultAdapter.clear();
        for (BluetoothGattService service : gatt.getServices()) {
            mResultAdapter.addResult(service);
        }
        mResultAdapter.notifyDataSetChanged();
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
