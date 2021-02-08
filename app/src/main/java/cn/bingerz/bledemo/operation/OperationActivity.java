package cn.bingerz.bledemo.operation;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cn.bingerz.bledemo.R;
import cn.bingerz.bledemo.comm.Observer;
import cn.bingerz.bledemo.comm.ObserverManager;
import cn.bingerz.bledemo.util.ServiceUUID;
import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.exception.BLEException;
import cn.bingerz.flipble.peripheral.Peripheral;
import cn.bingerz.flipble.peripheral.callback.NotifyCallback;
import cn.bingerz.flipble.peripheral.callback.ReadCallback;
import cn.bingerz.flipble.peripheral.callback.RssiCallback;
import cn.bingerz.flipble.peripheral.command.Command;

public class OperationActivity extends AppCompatActivity implements Observer {

    private static final String TAG = OperationActivity.class.toString();

    public static final String KEY_DATA = "key_data";

    private Peripheral mPeripheral;
    private BluetoothGattService bluetoothGattService;
    private BluetoothGattCharacteristic characteristic;
    private int charaProp;

    private Toolbar toolbar;
    private List<Fragment> fragments = new ArrayList<>();
    private int currentPage = 0;
    private String[] titles = new String[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_operation);
        initData();
        initView();
        initPage();

        ObserverManager.getInstance().addObserver(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_test, menu);
        getMenuInflater().inflate(R.menu.menu_retry_discover, menu);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_test:
                commandPressureTest();
                break;
            case R.id.action_retry_discover:
                retryDiscoverService();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPeripheral != null) {
            mPeripheral.clearCharacterCallback();
        }
        ObserverManager.getInstance().deleteObserver(this);
    }

    @Override
    public void disConnected(Peripheral peripheral) {
        if (peripheral != null && mPeripheral != null && peripheral.getAddress().equals(mPeripheral.getAddress())) {
            finish();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (currentPage != 0) {
                currentPage--;
                changePage(currentPage);
                return true;
            } else {
                finish();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void initView() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(titles[0]);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentPage != 0) {
                    currentPage--;
                    changePage(currentPage);
                } else {
                    finish();
                }
            }
        });
    }

    private void initData() {
        String key = getIntent().getStringExtra(KEY_DATA);
        if (TextUtils.isEmpty(key)) {
            finish();
        }

        mPeripheral = CentralManager.getInstance().getPeripheral(key);
        if (mPeripheral == null) {
            finish();
        }

        titles = new String[]{
                getString(R.string.service_list),
                getString(R.string.characteristic_list),
                getString(R.string.property_list),
                getString(R.string.console)};
    }

    private void initPage() {
        prepareFragment();
        changePage(0);
    }

    public void changePage(int page) {
        currentPage = page;
        toolbar.setTitle(titles[page]);
        updateFragment(page);
        if (currentPage == 1) {
            ((CharacteristicListFragment) fragments.get(1)).showData();
        } else if (currentPage == 2) {
            ((PropertyListFragment) fragments.get(2)).showData();
        } else if (currentPage == 3) {
            ((CharacteristicOperationFragment) fragments.get(3)).showData();
        }
    }

    private void prepareFragment() {
        fragments.add(new ServiceListFragment());
        fragments.add(new CharacteristicListFragment());
        fragments.add(new PropertyListFragment());
        fragments.add(new CharacteristicOperationFragment());
        for (Fragment fragment : fragments) {
            getSupportFragmentManager().beginTransaction().add(R.id.fragment, fragment).hide(fragment).commit();
        }
    }

    private void updateFragment(int position) {
        if (position > fragments.size() - 1) {
            return;
        }
        for (int i = 0; i < fragments.size(); i++) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            Fragment fragment = fragments.get(i);
            if (i == position) {
                transaction.show(fragment);
            } else {
                transaction.hide(fragment);
            }
            transaction.commit();
        }
    }

    public Peripheral getPeripheral() {
        return mPeripheral;
    }

    public BluetoothGattService getBluetoothGattService() {
        return bluetoothGattService;
    }

    public void setBluetoothGattService(BluetoothGattService bluetoothGattService) {
        this.bluetoothGattService = bluetoothGattService;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }

    public int getCharaProp() {
        return charaProp;
    }

    public void setCharaProp(int charaProp) {
        this.charaProp = charaProp;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void commandPressureTest() {
        if (mPeripheral == null) {
            return;
        }
        BluetoothGatt bluetoothGatt = mPeripheral.getBluetoothGatt();

        readRssiCommandTest();
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString(ServiceUUID.SERVER_DEVICE_INFO));
        if (service != null) {
            deviceInfoServiceCommandTest(service);
        }
        readRssiCommandTest();
        service = bluetoothGatt.getService(UUID.fromString(ServiceUUID.SERVICE_BATTERY));
        if (service != null) {
            batteryServiceCommandTest(service);
        }
        readRssiCommandTest();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void retryDiscoverService() {
        if (mPeripheral == null) {
            return;
        }
        BluetoothGatt bluetoothGatt = mPeripheral.getBluetoothGatt();
        if (bluetoothGatt != null) {
            bluetoothGatt.discoverServices();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void deviceInfoServiceCommandTest(BluetoothGattService service) {
        if (service == null) {
            return;
        }
        BluetoothGattCharacteristic charact = service.getCharacteristic(UUID.fromString(ServiceUUID.SYSTEM_ID));
        if (charact != null) {
            String serviceUUID = service.getUuid().toString();
            String charactUUID = charact.getUuid().toString();
            Command command = mPeripheral.createRead(Command.Priority.LOW, serviceUUID, charactUUID, new ReadCallback() {
                @Override
                public void onReadSuccess(byte[] data) {
                    Log.d(TAG, "Device info read systemId test success.");
                }

                @Override
                public void onReadFailure(BLEException exception) {
                    Log.d(TAG, "Device info read systemId test fail.");
                }
            });
            mPeripheral.read(command);
        }
        charact = service.getCharacteristic(UUID.fromString(ServiceUUID.FIRMWARE_VERSION));
        if (charact != null) {
            String serviceUUID = service.getUuid().toString();
            String charactUUID = charact.getUuid().toString();
            Command command = mPeripheral.createRead(Command.Priority.LOW, serviceUUID, charactUUID, new ReadCallback() {
                @Override
                public void onReadSuccess(byte[] data) {
                    Log.d(TAG, "Device info read firmware test success.");
                }

                @Override
                public void onReadFailure(BLEException exception) {
                    Log.d(TAG, "Device info read firmware test fail.");
                }
            });
            mPeripheral.read(command);
        }
        charact = service.getCharacteristic(UUID.fromString(ServiceUUID.HARDWARE_VERSION));
        if (charact != null) {
            String serviceUUID = service.getUuid().toString();
            String charactUUID = charact.getUuid().toString();
            Command command = mPeripheral.createRead(Command.Priority.HIGH, serviceUUID, charactUUID, new ReadCallback() {
                @Override
                public void onReadSuccess(byte[] data) {
                    Log.d(TAG, "Device info read hardware test success.");
                }

                @Override
                public void onReadFailure(BLEException exception) {
                    Log.d(TAG, "Device info read hardware test fail.");
                }
            });
            mPeripheral.read(command);
        }
        charact = service.getCharacteristic(UUID.fromString(ServiceUUID.MANUFACTURE_NAME));
        if (charact != null) {
            String serviceUUID = service.getUuid().toString();
            String charactUUID = charact.getUuid().toString();
            Command command = mPeripheral.createRead(Command.Priority.LOW, serviceUUID, charactUUID, new ReadCallback() {
                @Override
                public void onReadSuccess(byte[] data) {
                    Log.d(TAG, "Device info read manufacture test success.");
                }

                @Override
                public void onReadFailure(BLEException exception) {
                    Log.d(TAG, "Device info read manufacture test fail.");
                }
            });
            mPeripheral.read(command);
        }
        charact = service.getCharacteristic(UUID.fromString(ServiceUUID.PNP_ID));
        if (charact != null) {
            String serviceUUID = service.getUuid().toString();
            String charactUUID = charact.getUuid().toString();
            Command command = mPeripheral.createRead(Command.Priority.LOW, serviceUUID, charactUUID, new ReadCallback() {
                @Override
                public void onReadSuccess(byte[] data) {
                    Log.d(TAG, "Device info read pnp id test success.");
                }

                @Override
                public void onReadFailure(BLEException exception) {
                    Log.d(TAG, "Device info read pnp id test fail.");
                }
            });
            mPeripheral.read(command);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void batteryServiceCommandTest(BluetoothGattService service) {
        if (service == null) {
            return;
        }
        BluetoothGattCharacteristic charact = service.getCharacteristic(UUID.fromString(ServiceUUID.BATTERY_LEVEL));
        if (charact != null) {
            String serviceUUID = service.getUuid().toString();
            String charactUUID = charact.getUuid().toString();
            Command command = mPeripheral.createRead(Command.Priority.HIGH, serviceUUID, charactUUID, new ReadCallback() {
                @Override
                public void onReadSuccess(byte[] data) {
                    Log.d(TAG, "Battery level read test success.");
                }

                @Override
                public void onReadFailure(BLEException exception) {
                    Log.d(TAG, "Battery level read test fail.");
                }
            });
            mPeripheral.read(command);
        }
        charact = service.getCharacteristic(UUID.fromString(ServiceUUID.BATTERY_LEVEL));
        if (charact != null) {
            String serviceUUID = service.getUuid().toString();
            String charactUUID = charact.getUuid().toString();
            Command command = mPeripheral.createNotify(Command.Priority.HIGH, serviceUUID, charactUUID, true, new NotifyCallback() {
                @Override
                public void onNotifySuccess() {
                    Log.d(TAG, "Battery level notify enable test success.");
                }

                @Override
                public void onNotifyFailure(BLEException exception) {
                    Log.d(TAG, "Battery level notify enable test fail.");
                }

                @Override
                public void onCharacteristicChanged(byte[] data) {

                }
            });
            mPeripheral.notify(command);
        }
        charact = service.getCharacteristic(UUID.fromString(ServiceUUID.BATTERY_LEVEL));
        if (charact != null) {
            String serviceUUID = service.getUuid().toString();
            String charactUUID = charact.getUuid().toString();
            Command command = mPeripheral.createNotify(Command.Priority.MEDIUM, serviceUUID, charactUUID, false, new NotifyCallback() {
                @Override
                public void onNotifySuccess() {
                    Log.d(TAG, "Battery level notify disable test success.");
                }

                @Override
                public void onNotifyFailure(BLEException exception) {
                    Log.d(TAG, "Battery level notify disable test fail.");
                }

                @Override
                public void onCharacteristicChanged(byte[] data) {

                }
            });
            mPeripheral.notify(command);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void readRssiCommandTest() {
        Command command = mPeripheral.createReadRssi(Command.Priority.LOW, new RssiCallback() {
            @Override
            public void onRssiFailure(BLEException exception) {
                Log.d(TAG, "Read rssi test fail.");
            }

            @Override
            public void onRssiSuccess(int rssi) {
                Log.d(TAG, "Read rssi test success.");
            }
        });
        mPeripheral.readRssi(command);
    }
}
