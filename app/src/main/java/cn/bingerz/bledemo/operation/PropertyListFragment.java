package cn.bingerz.bledemo.operation;

import android.annotation.TargetApi;
import android.support.v4.app.Fragment;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.bingerz.bledemo.R;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class PropertyListFragment extends Fragment {

    private ResultAdapter mResultAdapter;
    private List<Integer> mPropertyList = new ArrayList<>();
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_property_list, null);
        initView(v);
        return v;
    }

    private void initView(View v) {
        mResultAdapter = new ResultAdapter();
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.rv_property_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(mResultAdapter);
    }

    public void showData() {
        BluetoothGattCharacteristic characteristic = ((OperationActivity) getActivity()).getCharacteristic();
        mResultAdapter.clear();
        mPropertyList.clear();

        int charaProp = characteristic.getProperties();
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            mPropertyList.add(CharacteristicOperationFragment.PROPERTY_READ);
            mResultAdapter.addResult("Read");
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            mPropertyList.add(CharacteristicOperationFragment.PROPERTY_WRITE);
            mResultAdapter.addResult("Write");
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
            mPropertyList.add(CharacteristicOperationFragment.PROPERTY_WRITE_NO_RESPONSE);
            mResultAdapter.addResult("Write No Response");
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            mPropertyList.add(CharacteristicOperationFragment.PROPERTY_NOTIFY);
            mResultAdapter.addResult("Notify");
        }
        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            mPropertyList.add(CharacteristicOperationFragment.PROPERTY_INDICATE);
            mResultAdapter.addResult("Indicate");
        }
        mResultAdapter.notifyDataSetChanged();
    }

    private class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {

        private List<String> propertyNameList;

        ResultAdapter() {
            propertyNameList = new ArrayList<>();
        }

        void addResult(String property) {
            propertyNameList.add(property);
        }

        void clear() {
            propertyNameList.clear();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_property, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final String propertyName = propertyNameList.get(position);
            final int property = mPropertyList.get(position);

            holder.tvTitle.setText(String.valueOf(getActivity().getString(R.string.property) + "（" + position + ")"));
            holder.tvProperty.setText(propertyName);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((OperationActivity) getActivity()).setCharaProp(property);
                    ((OperationActivity) getActivity()).changePage(3);
                }
            });
        }

        @Override
        public int getItemCount() {
            return propertyNameList == null ? 0 : propertyNameList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvProperty;
            ImageView ivNext;

            public ViewHolder(View itemView) {
                super(itemView);
                tvTitle = (TextView) itemView.findViewById(R.id.tv_title);
                tvProperty = (TextView) itemView.findViewById(R.id.tv_property);
                ivNext = (ImageView) itemView.findViewById(R.id.iv_next);
            }
        }
    }
}
