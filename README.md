# FlipBLE
[![Maven Central](https://img.shields.io/maven-central/v/cn.bingerz.android/flipble.svg)](https://search.maven.org/artifact/cn.bingerz.android/flipble)
[![Download](https://api.bintray.com/packages/bingerz/maven/flip-ble/images/download.svg)](https://bintray.com/bingerz/maven/flip-ble)

A stable and simple Bluetooth development framework for the Android platform.

## About FlipBLE
This framework is already in production. After several iterations, it has been relatively stable 
and reliable. I have been engaged in Android Bluetooth related app development for a long time, 
studied the Bluetooth protocol stack under Android, and also worked on the firmware development 
of Bluetooth chip. This framework was developed to increase the ease of use and stability of using 
the Android Bluetooth API.

## Feature
 - Support BLE devices for basic operations such as scanning, connect, read, write, notification subscription and cancellation.
 - Support custom scan mode (power saving, balance, power consumption), foreground background scanning mode, filtering rules, etc.
 - Support for periodic scanning function.
 - Support connection retry feature to increase connection success probability.
 - Support Bluetooth command timeout feature.
 - Supports Bluetooth command cache, priority features, multiple devices connected to execute commands more stable.

## Getting Started
### Add FlipBLE in your build.gradle
```groovy
dependencies {
    implementation 'cn.bingerz.android:flipble:0.6.1'
}
```

## About Central/Peripheral：
 - As the name suggests, Bluetooth is a master-slave device.
   For example, the use of mobile phones and sports bracelets, the mobile phone is central, the bracelet is peripheral;
   FlipBLE only supports mobile phones that are Central's working mode and do not support working mode as a peripheral.
 
### Initialization instance

```java
\\ Given the lifecycle of CentralManager, it is best to use the Application Context
\\ Can be initialized in the Activity and Service onCreate() method
CentralManager.getInstance().init(getApplicationContext());
CentralManager.getInstance()
              .enableLog(true) //Enable ble log
              .setOperateTimeout(5000) //Set the timeout period for Read and Write operations
              .setMaxConnectCount(7); //Set max number of connections, Default value:7
                                      //This is the maximum value defined in the Bluetooth protocol doc.
```
### Start/Stop Scanning
```java
List<ScanFilterConfig> scanFilterConfigs = new ArrayList<>();
ScanFilterConfig.Builder filterBuilder = new ScanFilterConfig.Builder();
filterBuilder.setDeviceMac(mac);    //Mac address you want to filter
filterBuilder.setDeviceName(name);  //The deviceName you want to filter
filterBuilder.setServiceUUID(uuid); //The ServiceUuid you want to filter
scanFilterConfigs.add(filterBuilder.build());

ScanRuleConfig mScanRuleConfig = new ScanRuleConfig.Builder()
    .setScanFilterConfigs(scanFilterConfigs)        // Scan only the specified device, optional
    .setScanMode(ScanRuleConfig.SCAN_MODE_BALANCED) // Scan mode, optional Default: low interval scan
    .setScanDuration(6000)                          // Scan duration, optional
    .setScanInterval(6000)                          // Scan interval, optional
    .build();
    
    //Start scanning
    //The first parameter of startScan, indicating whether this is a single or periodic scan
    CentralManager.getInstance().startScan(false, mScanRuleConfig, new ScanCallback() {
                @Override
                public void onScanStarted() {
                    //Processing start of scanning
                }
    
                @Override
                public void onScanning(ScanDevice device) {
                    //Handling scanned new devices
                }
    
                @Override
                public void onScanFinished(List<ScanDevice> scanResultList) {
                    //Scan completed, return to the list of scanned devices
                }
            });
    
    //Stop scanning
    CentralManager.getInstance().stopScan();
```
Mark:
1、The ScanInterval configuration is only relevant for periodic scans (CycledScanner) 
    and does not work for a single scan of OnceScanner.

### Connect/Disconnect Peripherals
```java
    Peripheral peripheral = new Peripheral(scanDevice);
    //isAutoConnect performs autoConnect connection, 
    // for which you can refer to the official Android documentation.
    // 
    peripheral.connect(isAutoConnect, new ConnectStateCallback() {
        @Override
        public void onStartConnect() {
            // Processing of connecting devices
        }

        @Override
        public void onConnectFail(BLEException exception) {
            // Connection failure processing
        }

        @Override
        public void onConnectSuccess(Peripheral peripheral, int status) {
            //Handling the connection successfully
        }

        @Override
        public void onDisConnected(boolean isActiveDisConnected, Peripheral peripheral, int status) {
            //Processing device disconnected
        }
    });
    
    peripheral.disconnect() // Disconnect peripherals
```

Regarding the AutoConnect parameters, according to the interpretation of the Bluetooth core protocol,
the following distinction can be made simply. This concurrency talks about the problem of the Bluetooth 
channel communication protocol level.
AutoConnect=false   Connection procedures cannot be concurrent.
AutoConnect=true    Connection procedures can be concurrent.

Example：
The APP initialization is completed. It is known to connect to the Mac address of a device, obtain 
the peripheral through the CentralManager method, and then perform the autoConnect connection.

```java
Peripheral peripheral = CentralManager.getInstance().retrievePeripheral(address);
peripheral.connect(true, new ConnectStateCallback(){
    /*someCode*/
});
```
After the system is connected to the device, the ConnectStateCallback method is called back.
Tips：The lower Android version connection completion time is slower, and the higher version will increase the connection speed.

### Peripherals operation is performed immediately
Call the following method, Bluetooth operation will be executed immediately.
```java
public void notify(String serviceUUID, String notifyUUID, NotifyCallback callback) {
    /*someCode*/
});
public boolean stopNotify(String serviceUUID, String notifyUUID) {
    /*someCode*/
});
public void indicate(String serviceUUID, String indicateUUID, IndicateCallback callback) {
    /*someCode*/
});
public boolean stopIndicate(String serviceUUID, String indicateUUID) {
    /*someCode*/
});
public void write(String serviceUUID, String writeUUID, byte[] data, WriteCallback callback) {
    /*someCode*/
});
public void read(String serviceUUID, String readUUID, ReadCallback callback) {
    /*someCode*/
});
public void readRssi(RssiCallback callback) {
    /*someCode*/
});
public void setMtu(int mtu, MtuChangedCallback callback) {
    /*someCode*/
});
```

### Peripherals Operation supports priority and cache features
Call the method, the operation supports priority and cache characteristics, and executes according to the set priority order. 
The cache instruction is determined according to whether all currently connected peripherals are busy, and execution is delayed.
```java
public void notify(Command command) {
    /*someCode*/
});
public void indicate(Command command) {
    /*someCode*/
});
public void write(Command command) {
    /*someCode*/
});
public void read(Command command) {
    /*someCode*/
});
public void readRssi(Command command) {
    /*someCode*/
});
public void setMtu(Command command) {
    /*someCode*/
});
```

## Release Changes
### v0.6.1
 - Improve scanner handle;
### v0.5.2
 - Bug fixs
### v0.5.1
 - Add Bluetooth command cache feature;
 - Improve instruction concurrency stability;
 - Increase the priority of instruction execution;
### v0.4.8
 - Add exception type
### v0.4.4
 - Bug fixs
### v0.4.3
 - Bug fixs
### v0.3.3
 - Bug fixs
### v0.3.2
 - Remove scan config's background mode
### v0.3.1
 - Init First Commit
 
## Android BLE API usage notes:
 - Some APIs must be paired, used in order, if they do not appear in pairs, causing Crash, resource 
   usage exceptions, and internal state exceptions. As a result, the device cannot be scanned and the 
   device cannot be connected. In severe cases, the phone needs to be restarted before it can be restored.

    startScan() & stopScan()
    
    connectGatt() & close()
    
    Tips: You can call disconnect() first, wait for the callback to be disconnected (onConnectionStateChange), and then execute close().
 
 - The BluetoothGatt class's read/write XXX set XXX and other APIs, when calling these methods, 
   to determine the completion of the previous operation, and then execute the next call, while 
   executing two, will cause the execution to fail.
 
  - BluetoothGatt method call, consider adding a retry operation, because the Android Bluetooth API 
    is internally executed by mDeviceBusy for a limited time. If an execution error occurs, you can 
    delay the attempt again.
 
  - The implementation of BluetoothGatt's readRemoteRssi method too frequently (such as a few hundred 
    milliseconds) will cause DeadObjects to be abnormal.
  
  - For Android6.0 and above, you must authorize ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION 
    permission before calling startLeScan.Otherwise, the Android Bluetooth interface calls an exception 
    that occurs directly inside the call and is not thrown.
    The phenomenon is to start scanning but does not return to the scanned device.
 
  - Call startLeScan and stopLeScan, make sure that Bluetooth is turned on, otherwise it will crash directly on some models.
 
  - Android8.1 and above to enable Bluetooth scanning, you must specify serviceUuid, or you will not
    be able to scan the device if you lock the screen.
 
  - Do not perform read, write, notify, etc. operations in callbacks such as onServicesDiscovered, 
    and put them on the main thread to execute.


