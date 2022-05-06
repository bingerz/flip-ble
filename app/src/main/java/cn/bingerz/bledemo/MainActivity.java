package cn.bingerz.bledemo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.espresso.IdlingResource;

import java.util.ArrayList;
import java.util.List;

import cn.bingerz.bledemo.adapter.ScanDeviceAdapter;
import cn.bingerz.bledemo.comm.ObserverManager;
import cn.bingerz.bledemo.operation.OperationActivity;
import cn.bingerz.bledemo.util.EspressoIdlingResource;
import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.scanner.ScanDevice;
import cn.bingerz.flipble.scanner.ScanFilterConfig;
import cn.bingerz.flipble.exception.BLEException;
import cn.bingerz.flipble.peripheral.Peripheral;
import cn.bingerz.flipble.peripheral.callback.ConnectStateCallback;
import cn.bingerz.flipble.peripheral.callback.MtuChangedCallback;
import cn.bingerz.flipble.peripheral.callback.RssiCallback;
import cn.bingerz.flipble.scanner.ScanRuleConfig;
import cn.bingerz.flipble.scanner.callback.ScanCallback;
import cn.bingerz.flipble.utils.GeneralUtil;

/**
 * @author hanson
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_OPEN_GPS = 1;
    private static final int REQUEST_CODE_PERMISSION_BLUETOOTH = 2;
    private static final int REQUEST_CODE_PERMISSION_LOCATION = 3;

    private LinearLayout llSetting;
    private TextView tvSetting;
    private Button btnScan;
    private EditText etName, etMac, etUUID;
    private Switch swAuto;
    private ImageView ivLoading;

    private Animation operatingAnim;
    private ScanDeviceAdapter mScanDeviceAdapter;
    private ProgressDialog progressDialog;

    private Peripheral mPeripheral;
    private ScanRuleConfig mScanRuleConfig;
    private String DEFAULT_SERVICE_UUID = "00001803-0000-1000-8000-00805f9b34fb";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        initView();

        CentralManager.getInstance().init(getApplicationContext());

        CentralManager.getInstance().enableLog(true).setMaxConnectCount(7).setOperateTimeout(5000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showConnectedDevice();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CentralManager.getInstance().destroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                if (btnScan.getText().equals(getString(R.string.start_scan))) {
                    checkPermissions();
                } else if (btnScan.getText().equals(getString(R.string.stop_scan))) {
                    stopScan();
                }
                break;
            case R.id.txt_setting:
                if (llSetting.getVisibility() == View.VISIBLE) {
                    llSetting.setVisibility(View.GONE);
                    tvSetting.setText(getString(R.string.expand_search_settings));
                } else {
                    llSetting.setVisibility(View.VISIBLE);
                    tvSetting.setText(getString(R.string.retrieve_search_settings));
                }
                break;
            default:
                break;
        }
    }

    private void initView() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnScan = findViewById(R.id.btn_scan);
        btnScan.setText(getString(R.string.start_scan));
        btnScan.setOnClickListener(this);

        etName = findViewById(R.id.et_name);
        etMac = findViewById(R.id.et_mac);
        etUUID = findViewById(R.id.et_uuid);
        swAuto = findViewById(R.id.sw_auto);

        llSetting = findViewById(R.id.layout_setting);
        tvSetting = findViewById(R.id.txt_setting);
        tvSetting.setOnClickListener(this);
        llSetting.setVisibility(View.GONE);
        tvSetting.setText(getString(R.string.expand_search_settings));

        ivLoading = findViewById(R.id.img_loading);
        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate);
        operatingAnim.setInterpolator(new LinearInterpolator());
        progressDialog = new ProgressDialog(this);

        mScanDeviceAdapter = new ScanDeviceAdapter();
        mScanDeviceAdapter.setOnDeviceClickListener(new ScanDeviceAdapter.OnDeviceClickListener() {
            @Override
            public void onConnect(ScanDevice device) {
                if (!CentralManager.getInstance().isConnected(device.getAddress())) {
                    EspressoIdlingResource.increment();
                    stopScan();
                    Peripheral peripheral = new Peripheral(device);
                    connect(peripheral);
                }
            }

            @Override
            public void onDisConnect(ScanDevice device) {
                if (CentralManager.getInstance().isConnected(device.getAddress())) {
                    Peripheral peripheral = CentralManager.getInstance().getPeripheral(device.getAddress());
                    if (peripheral != null) {
                        peripheral.disconnect();
                    }
                }
            }

            @Override
            public void onDetail(ScanDevice device) {
                if (CentralManager.getInstance().isConnected(device.getAddress())) {
                    Intent intent = new Intent(MainActivity.this, OperationActivity.class);
                    intent.putExtra(OperationActivity.KEY_DATA, device.getAddress());
                    startActivity(intent);
                }
            }
        });
        RecyclerView mRecyclerView = findViewById(R.id.rv_list);
        mRecyclerView.setHasFixedSize(true);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mScanDeviceAdapter);
    }

    private void showConnectedDevice() {
        List<Peripheral> deviceList = CentralManager.getInstance().getAllConnectedDevice();
        mScanDeviceAdapter.clearConnectedDevice();
        for (Peripheral peripheral : deviceList) {
            mScanDeviceAdapter.addDevice(peripheral.getDevice());
        }
        mScanDeviceAdapter.notifyDataSetChanged();
    }

    private void setScanRule() {
        String[] uuids;
        String strUUID = etUUID.getText().toString();
        //strUUID = DEFAULT_SERVICE_UUID;
        if (TextUtils.isEmpty(strUUID)) {
            uuids = null;
        } else {
            uuids = strUUID.split(",");
        }

        String[] names;
        String strName = etName.getText().toString();
        if (TextUtils.isEmpty(strName)) {
            names = null;
        } else {
            names = strName.split(",");
        }

        String[] macs;
        String strMac = etMac.getText().toString();
        if (TextUtils.isEmpty(strMac)) {
            macs = null;
        } else {
            macs = strMac.split(",");
        }

        int macLength = macs == null ? 0 : macs.length;
        int nameLength = names == null ? 0 : names.length;
        int uuidLength = uuids == null ? 0 : uuids.length;

        int length = Math.max(Math.max(uuidLength, nameLength), macLength);

        List<ScanFilterConfig> scanFilterConfigs = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            String mac = macs == null || i >= macs.length ? null : macs[i];
            String name = names == null || i >= names.length ? null : names[i];
            String uuid = uuids == null || i >= uuids.length ? null : uuids[i];
            ScanFilterConfig.Builder filterBuilder = new ScanFilterConfig.Builder();
            filterBuilder.setDeviceMac(mac);
            filterBuilder.setDeviceName(name);
            filterBuilder.setServiceUUID(uuid);
            scanFilterConfigs.add(filterBuilder.build());
        }

        mScanRuleConfig = new ScanRuleConfig.Builder()
                // 只扫描指定的设备，可选
                .setScanFilterConfigs(scanFilterConfigs)
                .setScanMode(ScanRuleConfig.SCAN_MODE_BALANCED)
                // 扫描持续时间，可选
                .setScanDuration(6000)
                // 扫描间隔时间，可选
                .setScanInterval(6000)
                .build();
    }

    private void startScan() {
        CentralManager.getInstance().startScan(false, mScanRuleConfig, new ScanCallback() {
            @Override
            public void onScanStarted() {
                mScanDeviceAdapter.clearScanDevice();
                mScanDeviceAdapter.notifyDataSetChanged();
                ivLoading.startAnimation(operatingAnim);
                ivLoading.setVisibility(View.VISIBLE);
                btnScan.setText(getString(R.string.stop_scan));
            }

            @Override
            public void onScanning(ScanDevice device) {
                mScanDeviceAdapter.addDevice(device);
                mScanDeviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onScanFinished(List<ScanDevice> scanResultList) {
                ivLoading.clearAnimation();
                ivLoading.setVisibility(View.INVISIBLE);
                btnScan.setText(getString(R.string.start_scan));
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.e(TAG, "onScanFailed errorCode = " + errorCode);
                String text = getString(R.string.scan_fail, errorCode + "");
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void stopScan() {
        CentralManager.getInstance().stopScan();
    }

    private ConnectStateCallback mConnectStateCallback = new ConnectStateCallback() {
        @Override
        public void onStartConnect() {
            progressDialog.show();
        }

        @Override
        public void onConnectFail(BLEException exception) {
            EspressoIdlingResource.decrement();
            ivLoading.clearAnimation();
            ivLoading.setVisibility(View.INVISIBLE);
            btnScan.setText(getString(R.string.start_scan));
            progressDialog.dismiss();
            Log.e(TAG, "Connection failed: " + exception.toString());
            Toast.makeText(MainActivity.this, getString(R.string.connect_fail), Toast.LENGTH_LONG).show();
        }

        @Override
        public void onConnectSuccess(Peripheral peripheral, int status) {
            EspressoIdlingResource.decrement();
            progressDialog.dismiss();
            mPeripheral = peripheral;
            mScanDeviceAdapter.addDevice(mPeripheral.getDevice());
            mScanDeviceAdapter.notifyDataSetChanged();

            mPeripheral.requestConnectionPriorityBalanced();

            readRssi(mPeripheral);
            setMtu(mPeripheral, 23);
        }

        @Override
        public void onDisConnected(boolean isActiveDisConnected, String address, int status) {
            progressDialog.dismiss();
            mScanDeviceAdapter.removeDevice(address);
            mScanDeviceAdapter.notifyDataSetChanged();

            if (!isActiveDisConnected) {
                Toast.makeText(MainActivity.this, getString(R.string.disconnected), Toast.LENGTH_LONG).show();
                ObserverManager.getInstance().notifyObserver(mPeripheral);
            }
        }
    };

    private void connect(Peripheral peripheral) {
        boolean isAutoConnect = swAuto.isChecked();
        peripheral.setDiscoverWithHighPriority(true);
        peripheral.connect(isAutoConnect, mConnectStateCallback);
    }

    private void readRssi(Peripheral peripheral) {
        peripheral.readRssi(new RssiCallback() {
            @Override
            public void onRssiFailure(BLEException exception) {
                Log.i(TAG, "onRssiFailure" + exception.toString());
            }

            @Override
            public void onRssiSuccess(int rssi) {
                Log.i(TAG, "onRssiSuccess: " + rssi);
            }
        });
    }

    private void setMtu(Peripheral peripheral, int mtu) {
        peripheral.setMtu(mtu, new MtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BLEException exception) {
                Log.i(TAG, "onsetMTUFailure" + exception.toString());
            }

            @Override
            public void onMtuChanged(int mtu) {
                Log.i(TAG, "onMtuChanged: " + mtu);
            }
        });
    }

    private void checkPermissions() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show();
            return;
        }
        if (GeneralUtil.isNeedBluetoothGrant() && !GeneralUtil.isBluetoothGranted(this)) {
            String[] permissions = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION_BLUETOOTH);
        } else if (GeneralUtil.isNeedLocationGrant() && !GeneralUtil.isLocationGranted(this)) {
            String[] permissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_PERMISSION_LOCATION);
        }
    }

    private void onPermissionGranted(String permission) {
        switch (permission) {
            case Manifest.permission.BLUETOOTH_SCAN:
            case Manifest.permission.BLUETOOTH_CONNECT:
                break;
            case Manifest.permission.ACCESS_FINE_LOCATION:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                    new AlertDialog.Builder(this)
                            .setTitle(R.string.notifyTitle)
                            .setMessage(R.string.gpsNotifyMsg)
                            .setNegativeButton(R.string.cancel,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            finish();
                                        }
                                    })
                            .setPositiveButton(R.string.setting,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                            startActivityForResult(intent, REQUEST_CODE_OPEN_GPS);
                                        }
                                    })

                            .setCancelable(false)
                            .show();
                } else {
                    setScanRule();
                    startScan();
                }
                break;
            default:
                break;
        }
    }

    private boolean checkGPSIsOpen() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
                setScanRule();
                startScan();
            }
        }
    }

    @Override
    public final void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_BLUETOOTH:
                checkPermissions();
            case REQUEST_CODE_PERMISSION_LOCATION:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            onPermissionGranted(permissions[i]);
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    @VisibleForTesting
    public IdlingResource getCountingIdlingResource() {
        return EspressoIdlingResource.getIdlingResource();
    }
}
