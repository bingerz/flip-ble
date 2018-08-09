package cn.bingerz.bledemo.operation;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.List;

import cn.bingerz.bledemo.R;
import cn.bingerz.flipble.exception.BLEException;
import cn.bingerz.flipble.peripheral.Peripheral;
import cn.bingerz.flipble.peripheral.callback.IndicateCallback;
import cn.bingerz.flipble.peripheral.callback.NotifyCallback;
import cn.bingerz.flipble.peripheral.callback.ReadCallback;
import cn.bingerz.flipble.peripheral.callback.WriteCallback;
import cn.bingerz.flipble.utils.HexUtil;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CharacteristicOperationFragment extends Fragment {

    public static final int PROPERTY_READ = 1;
    public static final int PROPERTY_WRITE = 2;
    public static final int PROPERTY_WRITE_NO_RESPONSE = 3;
    public static final int PROPERTY_NOTIFY = 4;
    public static final int PROPERTY_INDICATE = 5;

    private LinearLayout llContainer;

    private List<String> childList = new ArrayList<>();


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_characteric_operation, null);
        initView(v);
        return v;
    }

    private void initView(View v) {
        llContainer = (LinearLayout) v.findViewById(R.id.layout_container);
    }

    public void showData() {
        final Peripheral peripheral = ((OperationActivity) getActivity()).getPeripheral();
        final BluetoothGattCharacteristic characteristic = ((OperationActivity) getActivity()).getCharacteristic();
        final int charaProp = ((OperationActivity) getActivity()).getCharaProp();
        String child = characteristic.getUuid().toString() + String.valueOf(charaProp);

        for (int i = 0; i < llContainer.getChildCount(); i++) {
            llContainer.getChildAt(i).setVisibility(View.GONE);
        }
        if (childList.contains(child)) {
            llContainer.findViewWithTag(peripheral.getAddress() + characteristic.getUuid().toString() + charaProp).setVisibility(View.VISIBLE);
        } else {
            childList.add(child);

            View view = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation, null);
            view.setTag(peripheral.getAddress() + characteristic.getUuid().toString() + charaProp);
            LinearLayout layout_add = (LinearLayout) view.findViewById(R.id.layout_add);
            final TextView txt_title = (TextView) view.findViewById(R.id.tv_title);
            txt_title.setText(String.valueOf(characteristic.getUuid().toString() + getActivity().getString(R.string.data_changed)));
            final TextView txt = (TextView) view.findViewById(R.id.txt);
            txt.setMovementMethod(ScrollingMovementMethod.getInstance());

            switch (charaProp) {
                case PROPERTY_READ: {
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_button, null);
                    Button btn = (Button) view_add.findViewById(R.id.btn);
                    btn.setText(getActivity().getString(R.string.read));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            peripheral.read(characteristic.getService().getUuid().toString(),
                                    characteristic.getUuid().toString(),
                                    new ReadCallback() {

                                        @Override
                                        public void onReadSuccess(final byte[] data) {
                                            if (isAdded() && getActivity() != null)
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        String value = HexUtil.formatHexString(data, true);
                                                        if (!TextUtils.isEmpty(value)) {
                                                            txt.append(value);
                                                            txt.append("\n");
                                                            int offset = txt.getLineCount() * txt.getLineHeight();
                                                            if (offset > txt.getHeight()) {
                                                                txt.scrollTo(0, offset - txt.getHeight());
                                                            }
                                                        }
                                                    }
                                                });
                                        }

                                        @Override
                                        public void onReadFailure(final BLEException exception) {
                                            if (isAdded() && getActivity() != null)
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        txt.append(exception.toString());
                                                        txt.append("\n");
                                                        int offset = txt.getLineCount() * txt.getLineHeight();
                                                        if (offset > txt.getHeight()) {
                                                            txt.scrollTo(0, offset - txt.getHeight());
                                                        }
                                                    }
                                                });
                                        }
                                    });
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_WRITE: {
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_et, null);
                    final EditText et = (EditText) view_add.findViewById(R.id.et);
                    Button btn = (Button) view_add.findViewById(R.id.btn);
                    btn.setText(getActivity().getString(R.string.write));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String hex = et.getText().toString();
                            if (TextUtils.isEmpty(hex)) {
                                return;
                            }
                            peripheral.write(characteristic.getService().getUuid().toString(),
                                    characteristic.getUuid().toString(),
                                    HexUtil.hexStringToBytes(hex),
                                    new WriteCallback() {

                                        @Override
                                        public void onWriteSuccess() {
                                            if (isAdded() && getActivity() != null)
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        txt.append("write success");
                                                        txt.append("\n");
                                                        int offset = txt.getLineCount() * txt.getLineHeight();
                                                        if (offset > txt.getHeight()) {
                                                            txt.scrollTo(0, offset - txt.getHeight());
                                                        }
                                                    }
                                                });
                                        }

                                        @Override
                                        public void onWriteFailure(final BLEException exception) {
                                            if (isAdded() && getActivity() != null)
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        txt.append(exception.toString());
                                                        txt.append("\n");
                                                        int offset = txt.getLineCount() * txt.getLineHeight();
                                                        if (offset > txt.getHeight()) {
                                                            txt.scrollTo(0, offset - txt.getHeight());
                                                        }
                                                    }
                                                });
                                        }
                                    });
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_WRITE_NO_RESPONSE: {
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_et, null);
                    final EditText et = (EditText) view_add.findViewById(R.id.et);
                    Button btn = (Button) view_add.findViewById(R.id.btn);
                    btn.setText(getActivity().getString(R.string.write));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String hex = et.getText().toString();
                            if (TextUtils.isEmpty(hex)) {
                                return;
                            }
                            peripheral.write(characteristic.getService().getUuid().toString(),
                                    characteristic.getUuid().toString(),
                                    HexUtil.hexStringToBytes(hex),
                                    new WriteCallback() {

                                        @Override
                                        public void onWriteSuccess() {
                                            if (isAdded() && getActivity() != null)
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        txt.append("write success");
                                                        txt.append("\n");
                                                        int offset = txt.getLineCount() * txt.getLineHeight();
                                                        if (offset > txt.getHeight()) {
                                                            txt.scrollTo(0, offset - txt.getHeight());
                                                        }
                                                    }
                                                });
                                        }

                                        @Override
                                        public void onWriteFailure(final BLEException exception) {
                                            if (isAdded() && getActivity() != null)
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        txt.append(exception.toString());
                                                        txt.append("\n");
                                                        int offset = txt.getLineCount() * txt.getLineHeight();
                                                        if (offset > txt.getHeight()) {
                                                            txt.scrollTo(0, offset - txt.getHeight());
                                                        }
                                                    }
                                                });
                                        }
                                    });
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_NOTIFY: {
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_button, null);
                    final Button btn = (Button) view_add.findViewById(R.id.btn);
                    btn.setText(getActivity().getString(R.string.open_notification));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (btn.getText().toString().equals(getActivity().getString(R.string.open_notification))) {
                                btn.setText(getActivity().getString(R.string.close_notification));
                                peripheral.notify(characteristic.getService().getUuid().toString(),
                                        characteristic.getUuid().toString(),
                                        new NotifyCallback() {

                                            @Override
                                            public void onNotifySuccess() {
                                                if (isAdded() && getActivity() != null)
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            txt.append("notify success");
                                                            txt.append("\n");
                                                            int offset = txt.getLineCount() * txt.getLineHeight();
                                                            if (offset > txt.getHeight()) {
                                                                txt.scrollTo(0, offset - txt.getHeight());
                                                            }
                                                        }
                                                    });
                                            }

                                            @Override
                                            public void onNotifyFailure(final BLEException exception) {
                                                if (isAdded() && getActivity() != null)
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            txt.append(exception.toString());
                                                            txt.append("\n");
                                                            int offset = txt.getLineCount() * txt.getLineHeight();
                                                            if (offset > txt.getHeight()) {
                                                                txt.scrollTo(0, offset - txt.getHeight());
                                                            }
                                                        }
                                                    });
                                            }

                                            @Override
                                            public void onCharacteristicChanged(byte[] data) {
                                                if (isAdded() && getActivity() != null)
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            txt.append(HexUtil.formatHexString(characteristic.getValue(), true));
                                                            txt.append("\n");
                                                            int offset = txt.getLineCount() * txt.getLineHeight();
                                                            if (offset > txt.getHeight()) {
                                                                txt.scrollTo(0, offset - txt.getHeight());
                                                            }
                                                        }
                                                    });
                                            }
                                        });
                            } else {
                                btn.setText(getActivity().getString(R.string.open_notification));
                                peripheral.stopNotify(characteristic.getService().getUuid().toString(),
                                        characteristic.getUuid().toString());
                            }
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;

                case PROPERTY_INDICATE: {
                    View view_add = LayoutInflater.from(getActivity()).inflate(R.layout.layout_characteric_operation_button, null);
                    final Button btn = (Button) view_add.findViewById(R.id.btn);
                    btn.setText(getActivity().getString(R.string.open_notification));
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (btn.getText().toString().equals(getActivity().getString(R.string.open_notification))) {
                                btn.setText(getActivity().getString(R.string.close_notification));
                                peripheral.indicate(characteristic.getService().getUuid().toString(),
                                        characteristic.getUuid().toString(),
                                        new IndicateCallback() {

                                            @Override
                                            public void onIndicateSuccess() {
                                                if (isAdded() && getActivity() != null)
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            txt.append("indicate success");
                                                            txt.append("\n");
                                                            int offset = txt.getLineCount() * txt.getLineHeight();
                                                            if (offset > txt.getHeight()) {
                                                                txt.scrollTo(0, offset - txt.getHeight());
                                                            }
                                                        }
                                                    });

                                            }

                                            @Override
                                            public void onIndicateFailure(final BLEException exception) {
                                                if (isAdded() && getActivity() != null)
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            txt.append(exception.toString());
                                                            txt.append("\n");
                                                            int offset = txt.getLineCount() * txt.getLineHeight();
                                                            if (offset > txt.getHeight()) {
                                                                txt.scrollTo(0, offset - txt.getHeight());
                                                            }
                                                        }
                                                    });
                                            }

                                            @Override
                                            public void onCharacteristicChanged(byte[] data) {
                                                if (isAdded() && getActivity() != null)
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            txt.append(HexUtil.formatHexString(characteristic.getValue(), true));
                                                            txt.append("\n");
                                                            int offset = txt.getLineCount() * txt.getLineHeight();
                                                            if (offset > txt.getHeight()) {
                                                                txt.scrollTo(0, offset - txt.getHeight());
                                                            }
                                                        }
                                                    });
                                            }
                                        });
                            } else {
                                btn.setText(getActivity().getString(R.string.open_notification));
                                peripheral.stopIndicate(characteristic.getService().getUuid().toString(),
                                        characteristic.getUuid().toString());
                            }
                        }
                    });
                    layout_add.addView(view_add);
                }
                break;
            }
            llContainer.addView(view);
        }
    }
}
