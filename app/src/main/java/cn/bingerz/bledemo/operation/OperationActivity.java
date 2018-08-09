package cn.bingerz.bledemo.operation;


import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import cn.bingerz.bledemo.R;
import cn.bingerz.bledemo.comm.Observer;
import cn.bingerz.bledemo.comm.ObserverManager;
import cn.bingerz.flipble.central.CentralManager;
import cn.bingerz.flipble.peripheral.Peripheral;

public class OperationActivity extends AppCompatActivity implements Observer {

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
        if (TextUtils.isEmpty(key))
            finish();

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


}
