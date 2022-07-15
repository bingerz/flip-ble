package cn.bingerz.bledemo.operation;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cn.bingerz.bledemo.R;
import cn.bingerz.flipble.exception.BLEException;
import cn.bingerz.flipble.peripheral.Peripheral;
import cn.bingerz.flipble.peripheral.callback.IndicateCallback;
import cn.bingerz.flipble.peripheral.callback.NotifyCallback;
import cn.bingerz.flipble.peripheral.callback.ReadCallback;
import cn.bingerz.flipble.peripheral.callback.WriteCallback;
import cn.bingerz.flipble.utils.HexUtil;

/**
 * @author hanson
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ProtocolListFragment extends Fragment {

    private static final String TAG = ProtocolListFragment.class.getSimpleName();
    private Peripheral mPeripheral;
    private TextView tvDeviceInfo, tvDeviceStatus;
    private ProtocolAdapter mProtocolAdapter;

    private ProgressDialog mProgressDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_protocol_list, null);
        initView(v);
        showData();
        return v;
    }

    private void initView(View v) {
        tvDeviceInfo = (TextView) v.findViewById(R.id.tv_protocol_list_info);
        tvDeviceStatus = (TextView) v.findViewById(R.id.tv_protocol_list_status);

        mProtocolAdapter = new ProtocolAdapter();
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.rv_protocol_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(mProtocolAdapter);

        mProgressDialog = new ProgressDialog(getActivity());
    }

    public void showData() {
        mPeripheral = ((OperationActivity) getActivity()).getPeripheral();
        String name = mPeripheral.getName();
        String mac = mPeripheral.getAddress();

        tvDeviceInfo.setText(String.format("%s%s  %s%s", getString(R.string.name), name, getString(R.string.mac), mac));
        tvDeviceStatus.setText(String.format("%s %s", getString(R.string.status), getString(R.string.status_tips)));

        mProtocolAdapter.clear();
        initProtocolCommandData();
        mProtocolAdapter.notifyDataSetChanged();
    }

    private void showToast(String text) {
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
    }

    /**
     * Create Protocol command
     * @param title
     * @param desc
     * @param serviceUUID
     * @param characterUUID
     * @param type
     * @param data
     * @return
     */
    private ProtocolCommand createProtocolCommand(String title, String desc,
                                                  String serviceUUID, String characterUUID,
                                                  int type, String data) {
        ProtocolCommand command = new ProtocolCommand();
        command.title = title;
        command.desc = desc;
        command.serviceUUID = serviceUUID;
        command.characterUUID = characterUUID;
        command.commandType = type;
        command.commandData = data;
        return command;
    }

    private void handlerProtocolCommand(ProtocolCommand command) {
        byte[] data = HexUtil.hexStringToBytes(command.commandData);
        switch (command.commandType) {
            case BluetoothGattCharacteristic.PROPERTY_READ:
                mProgressDialog.show();
                mPeripheral.read(command.serviceUUID, command.characterUUID, new ReadCallback() {
                    @Override
                    public void onReadSuccess(byte[] data) {
                        mProgressDialog.dismiss();
                        showToast("Read Command Success");
                        if (tvDeviceStatus != null) {
                            String result = String.format("Read Data %s", HexUtil.formatHexString(data));
                            tvDeviceStatus.setText(String.format("%s %s", getString(R.string.status), result));
                        }
                    }

                    @Override
                    public void onReadFailure(BLEException exception) {
                        mProgressDialog.dismiss();
                        showToast(String.format(Locale.getDefault(), "Read Command Failure. %s(%d)",
                                exception.getMessage(), exception.getCode()));
                    }
                });
                break;
            case BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE:
                mProgressDialog.show();
                mPeripheral.write(command.serviceUUID, command.characterUUID, data, new WriteCallback() {
                    @Override
                    public void onWriteSuccess() {
                        mProgressDialog.dismiss();
                        showToast("WriteNoResponse Command Success");
                    }

                    @Override
                    public void onWriteFailure(BLEException exception) {
                        mProgressDialog.dismiss();
                        showToast(String.format(Locale.getDefault(), "WriteNoResponse Command Failure. %s(%d)",
                                exception.getMessage(), exception.getCode()));
                    }
                });
                break;
            case BluetoothGattCharacteristic.PROPERTY_WRITE:
                mProgressDialog.show();
                mPeripheral.write(command.serviceUUID, command.characterUUID, data, new WriteCallback() {
                    @Override
                    public void onWriteSuccess() {
                        mProgressDialog.dismiss();
                        showToast("Write Command Success");
                    }

                    @Override
                    public void onWriteFailure(BLEException exception) {
                        mProgressDialog.dismiss();
                        showToast(String.format(Locale.getDefault(), "Write Command Failure. %s(%d)",
                                exception.getMessage(), exception.getCode()));
                    }
                });
                break;
            case BluetoothGattCharacteristic.PROPERTY_NOTIFY:
                mProgressDialog.show();
                mPeripheral.notify(command.serviceUUID, command.characterUUID, new NotifyCallback() {
                    @Override
                    public void onNotifySuccess() {
                        mProgressDialog.dismiss();
                        showToast("Notify Command Success");
                    }

                    @Override
                    public void onNotifyFailure(BLEException exception) {
                        mProgressDialog.dismiss();
                        showToast(String.format(Locale.getDefault(), "Notify Command Failure. %s(%d)",
                                exception.getMessage(), exception.getCode()));
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        if (tvDeviceStatus != null) {
                            String result = String.format("Notify Data %s", HexUtil.formatHexString(data));
                            tvDeviceStatus.setText(String.format("%s %s", getString(R.string.status), result));
                        }
                    }
                });
                break;
            case BluetoothGattCharacteristic.PROPERTY_INDICATE:
                mProgressDialog.show();
                mPeripheral.indicate(command.serviceUUID, command.characterUUID, new IndicateCallback() {
                    @Override
                    public void onIndicateSuccess() {
                        mProgressDialog.dismiss();
                        showToast("Indicate Command Success");
                    }

                    @Override
                    public void onIndicateFailure(BLEException exception) {
                        mProgressDialog.dismiss();
                        showToast(String.format(Locale.getDefault(), "Indicate Command Failure. %s(%d)",
                                exception.getMessage(), exception.getCode()));
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] data) {
                        if (tvDeviceStatus != null) {
                            String result = String.format("Indicate Data %s", HexUtil.formatHexString(data));
                            tvDeviceStatus.setText(String.format("%s %s", getString(R.string.status), result));
                            Log.e(TAG, result);
                        }
                    }
                });
                break;
            default:
                break;
        }
    }

    private ProtocolCommand createProtocolCmd(String title, String desc, int type, String data) {
        String serviceUUID = "0000ff00-0000-1000-8000-00805f9b34fb";
        String characterUUID = "0000ff01-0000-1000-8000-00805f9b34fb";
        return createProtocolCommand(title, desc, serviceUUID, characterUUID, type, data);
    }

    private void initProtocolCommandData() {
        String title = "BLE Device Cmd 01";
        String desc = "ServiceUUID FF00 <-> Charact UUID FF01 <-> ";
        int commandType = BluetoothGattCharacteristic.PROPERTY_WRITE;
        String commandData = "04";
        mProtocolAdapter.addProtocolCommand(createProtocolCmd(title, desc, commandType, commandData));
    }

    private class ProtocolAdapter extends RecyclerView.Adapter<ProtocolAdapter.ViewHolder> {

        private List<ProtocolCommand> mProtocolCommands;

        ProtocolAdapter() {
            mProtocolCommands = new ArrayList<>();
        }

        void addProtocolCommand(ProtocolCommand command) {
            mProtocolCommands.add(command);
        }

        void clear() {
            mProtocolCommands.clear();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.adapter_protocol, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            final ProtocolCommand command = mProtocolCommands.get(position);
            if (command != null) {
                holder.tvTitle.setText(command.title);
                String type = "";
                switch (command.commandType) {
                    case BluetoothGattCharacteristic.PROPERTY_READ:
                        type = "Read";
                        break;
                    case BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE:
                        type = "WriteNoResponse";
                        break;
                    case BluetoothGattCharacteristic.PROPERTY_WRITE:
                        type = "Write";
                        break;
                    case BluetoothGattCharacteristic.PROPERTY_NOTIFY:
                        type = "Notify";
                        break;
                    case BluetoothGattCharacteristic.PROPERTY_INDICATE:
                        type = "Indicate";
                        break;
                    default:
                        break;
                }
                String desc = String.format("%s%s", command.desc, type);
                holder.tvDesc.setText(desc);
                holder.tvServiceUUID.setText(String.format("Service:%s", command.serviceUUID));
                holder.tvCharacterUUID.setText(String.format("Charact:%s", command.characterUUID));
                holder.tvProtocolData.setText(String.format("Data:%s", command.commandData));
            }

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handlerProtocolCommand(command);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mProtocolCommands == null ? 0 : mProtocolCommands.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle;
            TextView tvDesc;
            TextView tvServiceUUID;
            TextView tvCharacterUUID;
            TextView tvProtocolData;

            public ViewHolder(View itemView) {
                super(itemView);
                tvTitle = (TextView) itemView.findViewById(R.id.tv_title);
                tvDesc = (TextView) itemView.findViewById(R.id.tv_desc);
                tvServiceUUID = (TextView) itemView.findViewById(R.id.tv_service_uuid);
                tvCharacterUUID = (TextView) itemView.findViewById(R.id.tv_character_uuid);
                tvProtocolData = (TextView) itemView.findViewById(R.id.tv_protocol_data);
            }
        }
    }

    private class ProtocolCommand {
        public String title;
        public String desc;
        public String serviceUUID;
        public String characterUUID;
        public int commandType;
        public String commandData;
    }
}
