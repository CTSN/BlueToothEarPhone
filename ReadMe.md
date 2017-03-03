##Android开发之蓝牙连接与配对设备
###一、配置蓝牙权限
	<!--允许程序连接到已配对的蓝牙设备--!>
	<uses-permission android:name="android.permission.BLUETOOTH" />
	<!--允许程序发现和配对蓝牙设备--!>
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
**注意：当系统大于6.0时 开启蓝牙需要动态获取位置服务权限**

在onCreate中判断当前是否大于6.0
	
	if (Build.VERSION.SDK_INT >= 6.0) {
      	ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_REQUEST_CONSTANT);
   	}
   	
MY\_PERMISSION\_REQUEST\_CONSTANT是自己定义的常量值

动态权限配置回调

   	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_CONSTANT: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("main", "添加权限");
                    //permission granted!
                    //add some code
                }
                return;
            }
        }
    }
    
###二、开启蓝牙
获取BluetoothAdapter蓝牙适配器实例

	mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
通过startActivityForResult()方法发起开启蓝牙，根据常量值ACTION\_REQUEST\_ENABLE
	
   	/**如果本地蓝牙没有开启，则开启*/
   	if (!mBluetoothAdapter.isEnabled()) {
    	// 我们通过startActivityForResult()方法发起的Intent将会在onActivityResult()回调方法中获取用户的选择，比如用户单击了Yes开启，
        // 那么将会收到RESULT_OK的结果，
        // 如果RESULT_CANCELED则代表用户不愿意开启蓝牙
        Intent mIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(mIntent, ENABLE_BLUE);
  	} else {
        Toast.makeText(this, "蓝牙已开启", Toast.LENGTH_SHORT).show();
   	}
在onActivityResult()判断蓝牙是否开启成功（ENABLE_BLUE是自己定义的常量值）

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ENABLE_BLUE) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "蓝牙开启成功", Toast.LENGTH_SHORT).show();
                getBondedDevices();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "蓝牙开始失败", Toast.LENGTH_SHORT).show();
            }
        } else {

        }
    }
    
###三、设置蓝牙可见
根据常量值BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION，通过Intent开启广播的方式设置开启蓝牙可见。

	Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
    intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 180);//180可见时间 单位s
    startActivity(intent);
###四、搜索蓝牙
搜索蓝牙是根据注册广播接收者获取到搜索到的蓝牙设备信息。

相关常量值：BluetoothDevice.ACTION_FOUND、BluetoothAdapter.ACTION_DISCOVERY_FINISHED

BluetoothDevice是Android提供的蓝牙设备类。

1.创建广播接收者（BlueDevice是自定义的bean类）

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();


            /** 搜索到的蓝牙设备*/
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // 搜索到的不是已经配对的蓝牙设备
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    BlueDevice blueDevice = new BlueDevice();
                    blueDevice.setName(device.getName() == null ? device.getAddress() : device.getName());
                    blueDevice.setAddress(device.getAddress());
                    blueDevice.setDevice(device);
                    setDevices.add(blueDevice);
                    blueAdapter.setSetDevices(setDevices);
                    blueAdapter.notifyDataSetChanged();
                    Log.d(MAINACTIVITY, "搜索结果......" + device.getName() + device.getAddress());
                }
                /**当绑定的状态改变时*/
            } else if (action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                setProgressBarIndeterminateVisibility(false);
                Log.d(MAINACTIVITY, "搜索完成......");
                hideProgressDailog();
            }
        }
    };
    
2.注册广播接收

 	mFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    mFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);    //绑定状态监听
    mFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);     //搜索完成时监听
    registerReceiver(mReceiver, mFilter);
 
3.开启蓝牙搜索

	showProgressDailog();
    // 如果正在搜索，就先取消搜索
    if (mBluetoothAdapter.isDiscovering()) {
    	mBluetoothAdapter.cancelDiscovery();
    }
    Log.i("main", "我在搜索");
    // 开始搜索蓝牙设备,搜索到的蓝牙设备通过广播接受者返回
    mBluetoothAdapter.startDiscovery();
 
###五、配对蓝牙设备
 
 配对并连接蓝牙，本质就是跟设备进行数据交互。就是获取到BluetoothSocket，保持连接，然后进行数据交互。
 
**配对蓝牙设备方法有两种：可通过反射的方法端口 （1-30）和UUID进行操作**
 
 1.从上一步获取到的蓝牙设备列表中，选中其中一个BluetoothDevice，利用反射通过端口获得BluetoothSocket。
 
 *注意：获取socket是耗时操作，另启线程*
 
 	public void initSocket() {
        BluetoothSocket temp = null;
        try {
            Method m = blueDevice.getDevice().getClass().getMethod("createRfcommSocket", new Class[] {int.class});
            temp = (BluetoothSocket) m.invoke(blueDevice.getDevice(), 1);
        }catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        socket = temp;
    }

2.调用connet方法开始配对

	try {
         socket.connect();   //请求配对
    } catch (IOException e) {
         e.printStackTrace();
   	}
   	
3.取消配对，通过反射取消配对
	
	try {
          Method m = device.getClass().getMethod("removeBond", (Class[]) null);
          m.invoke(device, (Object[]) null);
   	} catch (Exception e) {
          Log.d("BlueUtils", e.getMessage());
    }

###六、连接蓝牙设备，操作设备
这里首先要区分下，配对与连接之间的区别。配对只是跟蓝牙设备之间起了个识别作用，获取到对方的名称、地址等信息，有能力建立起连接。

连接是当配对成功后，通过这几种固定的协议来连接操作蓝牙设备。

Android中提供的蓝牙协议：
A2DP协议、GATT协议、GATT_SERVER、HEADSET、 HEALTH、AP;

以上协议可以通过BluetoothProfile.XXX获取到。

本案例：演示通过A2DP协议连接操作蓝牙音箱。

1.创建一个协议监听对象BluetoothProfile.ServiceListener，监听是哪种协议服务。

	private BluetoothProfile.ServiceListener mProfileServiceListener = new BluetoothProfile.ServiceListener() {

        @Override
        public void onServiceDisconnected(int profile) {

        }

        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            try {
                    if (profile == BluetoothProfile.A2DP) {
                    /**使用A2DP的协议连接蓝牙设备（使用了反射技术调用连接的方法）*/
                    a2dp = (BluetoothA2dp) proxy;
                    if (a2dp.getConnectionState(currentBluetoothDevice) != BluetoothProfile.STATE_CONNECTED) {
                        a2dp.getClass()
                                .getMethod("connect", BluetoothDevice.class)
                                .invoke(a2dp, currentBluetoothDevice);
                        Toast.makeText(MainActivity.this, "请播放音乐", Toast.LENGTH_SHORT).show();
                        getBondedDevices();
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };	

2.开始连接蓝牙设备，通过A2DP协议连接（BluetoothProfile.A2DP）。
	
	mBluetoothAdapter.getProfileProxy(this, mProfileServiceListener, BluetoothProfile.A2DP);
	


操作过程至此结束。