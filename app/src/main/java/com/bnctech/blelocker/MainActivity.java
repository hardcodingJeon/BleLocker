package com.bnctech.blelocker;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.bnctech.blelocker.HttpsProtocol.RES_CODE__NOT_MATCHED_IDPW;
import static com.bnctech.blelocker.HttpsProtocol.RES_CODE__SUCCESS;
import static com.bnctech.blelocker.HttpsProtocol.RES_CODE__UPDATE;
import static com.bnctech.blelocker.HttpsProtocol.appServerBody_AdminPhoneKey;
import static com.bnctech.blelocker.HttpsProtocol.appServerBody_AppVersion;
import static com.bnctech.blelocker.HttpsProtocol.appServerBody_QRcodeKey;
import static com.bnctech.blelocker.HttpsProtocol.appServerBody_RequestMessageKey;
import static com.bnctech.blelocker.HttpsProtocol.appServerBody_RequestMessageValue_AdminConfirm;
import static com.bnctech.blelocker.HttpsProtocol.appServerBody_RequestMessageValue_GetQRcode;
import static com.bnctech.blelocker.HttpsProtocol.https_request;

public class MainActivity extends AppCompatActivity {
    public static final String appUpdateUrl = "https://play.google.com/store/apps/details?id=com.windroads.smartlocker";

    public static String appVersionName;

    public static int window_width;
    private FragmentManager fm;

    private DrawerLayout MainActivity_drawer;
    private LinearLayout MainActivity_LinearLayout_Version;
    private ImageView MainActivity_ImageView_Apps;
    private FloatingActionButton MainActivity_FAB_Apps, MainActivity_FAB_UnLock, MainActivity_FAB_LockConfirm;
    private TextView MainActivity_TextView_ReturnMsg;
    private TextView MainActivity_TextView_Time;

    private boolean isFabOpen = false;
    private Animation fab_open, fab_close, fab_rotate, fab_reverse;

    private static final int CAMERA_REQUEST = 1338;
    private static final int READ_PHONE_REQUEST = 1339;
    private static final int INITIAL_REQUEST = 1337;

    private static final int ACTIVITY_REQUEST_CODE_QR_SCAN  = 0x0000c0de;
    private static final int ACTIVITY_REQUEST_CODE_BT       = 1;

    private ArrayList<BluetoothGattService> mGattServices = new ArrayList<>();
    private ArrayList<BluetoothGattCharacteristic> mGattCharacteristics = new ArrayList<>();
    private ArrayList<BluetoothGattCharacteristic> mWritableCharacteristics = new ArrayList<>();
    private BluetoothGattCharacteristic mDefaultChar = null;
    private static final String service_uuid    = "8650c0e0-d046-4c6b-8a85-f82dba16ec71";
    private static final String data_uuid       = "2cf2c0e1-3769-40da-90a9-cbf3da480e65";
    private static final String descriptor_uuid = "00002902-0000-1000-8000-00805f9b34fb";
    private String notification_string;
    private AlertDialog.Builder responseDialog;

    private String BLE_Con_Data_QR_Code;
    private String BLE_Con_Data_Device_ID;
    private String BLE_Con_Data_Device_MAC;
    private String BLE_Con_Data_Auth_Key;

    private static final int STATE_DISCONNECTED      = 0;
    private static final int STATE_SCANNING          = 1;
    private static final int STATE_CONNECTING        = 2;
    private static final int STATE_CONNECTED         = 3;
    private static final int STATE_SERVICE_FOUND     = 4;
    private static final int STATE_SERVICE_NOT_FOUND = 5;
    private int mConnectionState = STATE_DISCONNECTED;

    private static final int ERROR_CODE_NO_ERROR          = 0;
    private static final int ERROR_CODE_DEVICE_NOT_FOUND  = 1;
    private static final int ERROR_CODE_GATT_DISCONNECTED = 2;
    private static final int ERROR_CODE_NO_SERVICE        = 3;
    private static final int ERROR_CODE_WRITE_FAIL        = 4;
    private static final int ERROR_CODE_OTHER             = 5;
    private int mBluetoothErrorCode = ERROR_CODE_NO_ERROR;

    private static final int COMMAND_MODE_UNLOCK = 8;
    private static final int COMMAND_MODE_RETURN = 9;
    private static final int COMMAND_MODE_INIT   = 0;
    private static final int COMMAND_MODE_WAIT   = 1;
    private static final int COMMAND_MODE_REPAIR = 3;
    private static final int COMMAND_MODE_TEST   = 5;
    private int mCommandMode = COMMAND_MODE_UNLOCK;

    private BluetoothLeScanner mBluetoothScanner;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice mDevice = null;
    private Handler mBluetoothScanHandler = new Handler();

    private static final long SCAN_PERIOD = 10000;

    public static final String tag_PhoneNumber = "PNB";
    public static String userData_PhoneNumber = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        appVersionName = SplashActivity.appVersionName;

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        window_width = dm.widthPixels;

        MainActivity_drawer = findViewById(R.id.MainActivity_DrawerView);
        MainActivity_TextView_ReturnMsg = findViewById(R.id.MainActivity_TextView_ReturnMsg);
        MainActivity_TextView_Time = findViewById(R.id.MainActivity_TextView_Time);

        fm = getSupportFragmentManager();

        fab_open = AnimationUtils.loadAnimation(this, R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(this, R.anim.fab_close);
        fab_rotate = AnimationUtils.loadAnimation(this, R.anim.fab_rotate);
        fab_reverse = AnimationUtils.loadAnimation(this, R.anim.fab_reverse);

        // Bluetooth
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            AlertDialog.Builder ab = new AlertDialog.Builder(MainActivity.this);
            ab.setCancelable(false);
            ab.setTitle("미지원");
            ab.setMessage("지원되지 않는 단말기입니다.");
            ab.setPositiveButton("확인", (dialog, which) -> {
                finish();
            });
            ab.show();
        }
        bluetooth_power_check();
        try {
            final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            mBluetoothAdapter = bluetoothManager.getAdapter();
            mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();
        } catch (Exception e) {
            Log.e("mBluetooth", e.toString());
        }

        new Task_AdminConfirm().execute();

        if (!canAccessFineLocation() || !canAccessCoarseLocation()) {
            requestPermissions(INITIAL_PERMS, INITIAL_REQUEST);
        }

        MainActivity_ImageView_Apps = findViewById(R.id.MainActivity_ImageView_Apps);
        MainActivity_ImageView_Apps.setOnClickListener(view -> {
            if (!MainActivity_drawer.isDrawerOpen(GravityCompat.END)) {
                MainActivity_drawer.openDrawer(GravityCompat.END);
            }
        });

        MainActivity_LinearLayout_Version = findViewById(R.id.MainActivity_LinearLayout_Version);
        MainActivity_LinearLayout_Version.setOnClickListener(view -> {
            CustomAlertWindowVersion alert = new CustomAlertWindowVersion();
            alert.show(fm, "");
        });

        MainActivity_FAB_Apps = findViewById(R.id.MainActivity_FAB_Apps);
        MainActivity_FAB_Apps.setOnClickListener(view -> {
            toggleFab();
            MainActivity_FAB_Apps.startAnimation(isFabOpen?fab_rotate:fab_reverse);
        });

        MainActivity_FAB_UnLock = findViewById(R.id.MainActivity_FAB_UnLock);
        MainActivity_FAB_UnLock.setOnClickListener(view -> {
            mCommandMode = COMMAND_MODE_UNLOCK;
            if (canCamera()) {
                QRCodeScan();
            } else {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
            }
        });

        MainActivity_FAB_LockConfirm = findViewById(R.id.MainActivity_FAB_LockConfirm);
        MainActivity_FAB_LockConfirm.setOnClickListener(view -> {
            if (BLE_Con_Data_QR_Code == null) {
                Toast.makeText(this, "락커를 먼저 열어주세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            mCommandMode = COMMAND_MODE_RETURN;
            new Task_GetBLEData().execute();
        });

        responseDialog = new AlertDialog.Builder(this);
    }

    public String getTime() {
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss");
        return sdf.format(date);
    }

    public void readPhoneNumber() {
        try {
            TelephonyManager telManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
            String phoneNumberStr = telManager.getLine1Number();

            if (phoneNumberStr.length() == 0) {
                Alert_Notification(MainActivity.this, true, "종료", "경고", "전화번호가 없는 단말기는 이용할 수 없습니다.\n프로그램을 종료합니다.");
            } else if (phoneNumberStr.contains("+82")) {
                userData_PhoneNumber = phoneNumberStr.replace("+82", "0");
            } else {
                userData_PhoneNumber = phoneNumberStr;
            }

            if (userData_PhoneNumber.length() != 11) {
                Alert_Notification(MainActivity.this, true, "종료", "전화번호", "전화번호 읽기에 실패하였습니다.");
            }

            SharedPreferences pref_PNB = getSharedPreferences(tag_PhoneNumber, Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor_PNB = pref_PNB.edit();
            editor_PNB.putString(tag_PhoneNumber, userData_PhoneNumber);
            if (!editor_PNB.commit()) {

            }

        } catch (Exception e) {
            Log.e("readPhoneNumber", e.toString());
            Alert_Notification(MainActivity.this, true, "종료", "USIM ERROR", "이용할 수 없는 단말기입니다.");
        }
    }

    private void toggleFab() {

        if (isFabOpen) {
            MainActivity_FAB_UnLock.startAnimation(fab_close);
            MainActivity_FAB_LockConfirm.startAnimation(fab_close);
            MainActivity_FAB_UnLock.setClickable(false);
            MainActivity_FAB_LockConfirm.setClickable(false);
            isFabOpen = false;
        } else {
            MainActivity_FAB_UnLock.startAnimation(fab_open);
            MainActivity_FAB_LockConfirm.startAnimation(fab_open);
            MainActivity_FAB_UnLock.setClickable(true);
            MainActivity_FAB_LockConfirm.setClickable(true);
            isFabOpen = true;
        }
    }

    // Internet Check
    private boolean internetCheck(boolean finish) {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        boolean rtnValue = (connectivityManager.getActiveNetworkInfo() != null);

        if (!rtnValue) {
            if (finish) {
                runOnUiThread(() -> {
                    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                    alert.setPositiveButton("확인", (dialog, which) -> finish());
                    alert.setTitle("알림");
                    alert.setMessage("인터넷 연결이 필요합니다.\n인터넷 연결 후 앱을 다시 실행해주세요.");
                    alert.setCancelable(false);
                    alert.show();
                });
            } else {
                runOnUiThread(() -> {
                    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                    alert.setPositiveButton("확인", null);
                    alert.setTitle("알림");
                    alert.setMessage("인터넷 연결이 필요합니다.");
                    alert.setCancelable(false);
                    alert.show();
                });
            }
        }

        return rtnValue;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case INITIAL_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Alert_Permission(this);
                }
                break;

            case CAMERA_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    bluetooth_power_check();
                    if (mBluetoothAdapter.getState() == BluetoothAdapter.STATE_ON) {
                        bluetooth_initialize();
                        QRCodeScan();
                    }
                } else {
                    Alert_Permission(this);
                }
                break;

            case READ_PHONE_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readPhoneNumber();
                } else {
                    Alert_Permission(this);
                }
        }
    }

    private void Alert_Permission(Context context) {
        AlertDialog.Builder ab = new AlertDialog.Builder(context);
        ab.setTitle("권한설정");
        ab.setCancelable(false);

        ab.setPositiveButton("확인", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID));
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            startActivity(intent);
            finishAffinity();
            System.exit(0);
        });
        ab.setMessage("다시 묻지 않음을 선택하셨습니다.\nAPP 사용을 위해 권한을 허가해주세요.\n확인을 누르시면 설정창으로 이동합니다.");
        ab.show();
    }

    private void bluetooth_power_check() {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ACTIVITY_REQUEST_CODE_BT);
        }
    }

    // Intent Result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch(requestCode) {
            case ACTIVITY_REQUEST_CODE_BT :
                bluetooth_power_check();
                try {
                    final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                    mBluetoothAdapter = bluetoothManager.getAdapter();
                    mBluetoothScanner = mBluetoothAdapter.getBluetoothLeScanner();
                } catch (Exception e) {
                    Log.e("mBluetooth", e.toString());
                }
                break;

            case ACTIVITY_REQUEST_CODE_QR_SCAN :
                IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
                String qr_string = result.getContents();

                if (!TextUtils.isEmpty(qr_string)) {
                    if (qr_string.contains("EBDLBA")) {
                        if ((mCommandMode == COMMAND_MODE_UNLOCK) || (mCommandMode == COMMAND_MODE_WAIT)) { /* 잠금해제, 대기로 상태 변경 */
                            BLE_Con_Data_QR_Code = qr_string;
                            new Task_GetBLEData().execute();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "유니콘바이크가 아닙니다.", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    // QR Code Scanner
    private void QRCodeScan() {
        String camera_id = getBackFacingCameraId();

        AlertDialog.Builder ab = new AlertDialog.Builder(this);
        ab.setCancelable(false);
        ab.setTitle("카메라");
        ab.setPositiveButton("확인", null);

        if (camera_id != null) {
            try {
                IntentIntegrator integrator = new IntentIntegrator(this);
                integrator.setCaptureActivity(CustomScannerActivity.class);
                integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES);
                integrator.setBeepEnabled(false);
                integrator.initiateScan();
            } catch (Exception e) {
                Log.e("QRCodeScan", e.toString());
                ab.setMessage("카메라에 문제가 발생하였습니다.");
                ab.show();
            }
        } else {
            ab.setMessage("카메라 불러오기에 실패하였습니다.");
            ab.show();
        }
    }

    private String getBackFacingCameraId() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            for (final String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (cOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                    return cameraId;
                }
            }
        } catch (Exception e) {
            Log.e("getBackFacingCameraId", e.toString());
        }

        return null;
    }

    // Alert Message Box
    private void Alert_Notification(Context context, boolean finish, String button, String title, String message) {
        AlertDialog.Builder ab = new AlertDialog.Builder(context);
        if (finish) {
            ab.setPositiveButton(button, (dialog, which) -> {
                finishAffinity();
                System.exit(0);
            });
        } else {
            ab.setPositiveButton(button, null);
        }
        ab.setTitle(title);
        ab.setMessage(message);
        ab.setCancelable(false);
        ab.show();
    }

    // Permission
    private static final String[] INITIAL_PERMS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private static final String[] READ_PHONE_PERMS = {
            Manifest.permission.READ_PHONE_NUMBERS
    };

    private static final String[] READ_SMS = {
            Manifest.permission.READ_SMS
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean canAccessFineLocation() {
        return (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean canAccessCoarseLocation() {
        return (hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean canReadPhoneNumbers() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return (hasPermission(Manifest.permission.READ_PHONE_NUMBERS));
        } else {
            return (hasPermission(Manifest.permission.READ_SMS));
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean canCamera() {
        return (hasPermission(Manifest.permission.CAMERA));
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private boolean hasPermission(String perm) {
        return (PackageManager.PERMISSION_GRANTED == checkSelfPermission(perm));
    }

    private void Alert_Update(Context context) {
        AlertDialog.Builder ab = new AlertDialog.Builder(context);
        ab.setPositiveButton("업데이트", (dialog, which) -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse(appUpdateUrl);
            intent.setData(uri);
            startActivity(intent);
            finishAffinity();
            System.exit(0);
        });
        ab.setTitle("업데이트");
        ab.setMessage("새로운 버전이 등록되었습니다.\n업데이트 버튼을 누르시면 업데이트 화면으로 이동합니다.\n새로워진 기능을 만나보세요.");
        ab.setCancelable(false);
        ab.show();
    }

    @Override
    public void onBackPressed() {
        if (MainActivity_drawer.isDrawerOpen(GravityCompat.END)) {
            MainActivity_drawer.closeDrawer(GravityCompat.END);
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomAlertDialogStyle);
        builder.setTitle("종료하시겠습니까?");
        builder.setPositiveButton("아니요",null);
        builder.setNegativeButton("예",(dialogInterface, i) -> finishAndRemoveTask());
        builder.show();
    }

    // bluetooth

    private void bluetooth_initialize() {
        mGattServices.clear();
        mWritableCharacteristics.clear();
        mGattCharacteristics.clear();
        mConnectionState    = STATE_SCANNING;
        mBluetoothErrorCode = ERROR_CODE_NO_ERROR;
        mDefaultChar        = null;
        notification_string = null;
    }

    private void bluetooth_disconnect() {
        try {
            if (mBluetoothGatt != null) {
                mBluetoothGatt.disconnect();
                Thread.sleep(200);
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            }
        } catch (InterruptedException e) {
            Log.e("bluetooth_disconnect", e.toString());
        }
    }

    private void scanLeDeivce(boolean enable) {
        if (enable) {
            mBluetoothScanHandler.postDelayed(() -> mBluetoothScanner.stopScan(mScanCallback), SCAN_PERIOD);
            mBluetoothScanner.startScan(mScanCallback);
        } else {
            mBluetoothScanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                processResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
        }

        private void processResult(final ScanResult result) {
            runOnUiThread(() -> {
                BluetoothDevice device = result.getDevice();
                if (device.getName() != null) {
                    if (device.getName().equals(BLE_Con_Data_Device_ID)) {
                        mDevice = device;
                        mConnectionState = STATE_CONNECTING;
                        scanLeDeivce(false);
                        connectToDevice(mDevice);
                    }
                }
            });
        }
    };

    public void connectToDevice(BluetoothDevice device) {
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.v("Bluetooth GATT", "Connected to GATT server.");
                mBluetoothGatt.discoverServices();

                mConnectionState = STATE_CONNECTED;
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.v("Bluetooth GATT", "Disconnected from GATT server.");

                mConnectionState = STATE_DISCONNECTED;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v("Bluetooth Discovery", "Discovery Service Start");

                checkGattServices(mBluetoothGatt.getServices());
            } else {
                Log.w("Bluetooth Discovery", "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.v("Bluetooth cc", "Read");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            notification_string = characteristic.getStringValue(0);
            Log.e("notification_string_1",notification_string);
        }
    };

    private int checkGattServices(List<BluetoothGattService> gattServices) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.v("GattService","# BluetoothAdapter not initialized");
            return -1;
        }

        for(BluetoothGattService gattService : gattServices) {
            mGattServices.add(gattService);
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            for(BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                mGattCharacteristics.add(gattCharacteristic);

                if(gattCharacteristic.getUuid().toString().equals(data_uuid)) {
                    boolean isWritable = isWritableCharacteristic(gattCharacteristic);
                    if (isWritable) {
                        mWritableCharacteristics.add(gattCharacteristic);
                    }

                    boolean isReadable = isReadableCharacteristic(gattCharacteristic);
                    if (isReadable) {
                        readCharacteristic(gattCharacteristic);
                    }

                    if (isNotificationCharacteristic(gattCharacteristic)) {
                        setCharacteristicNotification(gattCharacteristic, true);
                        if (isWritable && isReadable) {
                            mDefaultChar = gattCharacteristic;
                        }
                    }

                    mConnectionState = STATE_SERVICE_FOUND;
                    break;
                }
            }

            if (mConnectionState == STATE_SERVICE_FOUND) {
                break;
            }
        }

        if (mConnectionState != STATE_SERVICE_FOUND) {
            mConnectionState = STATE_SERVICE_NOT_FOUND;
        }

        return mGattCharacteristics.size();
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.v("ReadCharacteristic", "# BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null)  {
            Log.v("setChracteristic", "# BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(descriptor_uuid));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    private boolean isWritableCharacteristic(BluetoothGattCharacteristic chr) {
        if (chr == null) {
            return false;
        }

        final int charaProp = chr.getProperties();

        if (((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) | (charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) {
            Log.v("GattService","# Found writable characteristic");
            return true;
        } else {
            Log.v("GattService","# Not writable characteristic");
            return false;
        }
    }

    private boolean isReadableCharacteristic(BluetoothGattCharacteristic chr) {
        if (chr == null) {
            return false;
        }

        final int charaProp = chr.getProperties();

        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            Log.v("GattService","# Found readable characteristic");
            return true;
        } else {
            Log.v("GattService","# Not readable characteristic");
            return false;
        }
    }

    private boolean isNotificationCharacteristic(BluetoothGattCharacteristic chr) {
        if (chr == null) {
            return false;
        }

        final int charaProp = chr.getProperties();

        if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            Log.v("GattService","# Found notification characteristic");
            return true;
        } else {
            Log.v("GattService","# Not notification characteristic");
            return false;
        }
    }

    public boolean bluetooth_data_send(String str) {
        try {
            BluetoothGattService mGattService = mBluetoothGatt.getService(UUID.fromString(service_uuid));

            if (mGattServices.size() == 0) {
                Log.v("Service Con", "Custom BLE Service not found");
                return false;
            } else {
                Log.v("Service Con", "Custom BLE Service success");
            }

            BluetoothGattCharacteristic mGattCharacteristic = mGattService.getCharacteristic(UUID.fromString(data_uuid));
            mGattCharacteristic.setValue(str);
            mGattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);

            if (mBluetoothGatt.writeCharacteristic(mGattCharacteristic) == false) {
                Log.v("Service Con", "Failed to write characteristic");
                return false;
            } else {
                return true;
            }
        } catch(Exception e) {
            Log.v("Bluetooth send", e.toString());
            return false;
        }
    }

    private class Task_AdminConfirm extends AsyncTask<Void, Void, Void> {
        int resCode;
        String resMsg;
        ProgressDialog pDialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pDialog.setCancelable(false);
            pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pDialog.setCanceledOnTouchOutside(false);
            pDialog.setMessage("관리자 확인 중");
            pDialog.show();

            if(!internetCheck(true)) {
                this.cancel(true);
            }

            if (!canReadPhoneNumbers()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    requestPermissions(READ_PHONE_PERMS, READ_PHONE_REQUEST);
                } else {
                    requestPermissions(READ_SMS, READ_PHONE_REQUEST);
                }
                this.cancel(true);
            } else {
                readPhoneNumber();
            }

        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.e("read_phone_numbers_4","진입");
            try {
                JSONObject body = new JSONObject();
                body.put(appServerBody_AppVersion, appVersionName);
                body.put(appServerBody_RequestMessageKey, appServerBody_RequestMessageValue_AdminConfirm);
                body.put(appServerBody_AdminPhoneKey, userData_PhoneNumber);

                Object[] rtnValue = https_request(body);
                resCode = (int) rtnValue[0];
                resMsg = (String) rtnValue[1];
            } catch (Exception e) {
                Log.e("Task_AdminConfirm", e.toString());
            }

            return null;
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            pDialog.dismiss();
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            pDialog.dismiss();

            switch (resCode) {
                case RES_CODE__SUCCESS:
                    try {
                        JSONObject jsonObject = new JSONObject(resMsg);
//                        user_type = jsonObject.getInt("type");
                    } catch (Exception e) {
                        Log.e("Task_AdminConfirm", e.toString());
                    }
                    break;

                case RES_CODE__UPDATE:
                    Alert_Notification(MainActivity.this, true, "확인", "버전", "업데이트가 필요합니다.");
                    break;

                case RES_CODE__NOT_MATCHED_IDPW:
                    Alert_Notification(MainActivity.this, true, "확인", "관리자", "등록되지 않은 관리자 입니다.");
                    break;

                default:
                    Alert_Notification(MainActivity.this, true, "확인", "오류", "오류가 발생하였습니다.");
                    break;
            }
        }
    }

    private class Task_GetBLEData extends AsyncTask<Void, String, Void> {
        int resCode;
        String resMsg;
        ProgressDialog pDialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pDialog.setCancelable(false);
            pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            pDialog.setCanceledOnTouchOutside(false);
            pDialog.setMessage("서버에 접속중입니다.");
            pDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {
                JSONObject body = new JSONObject();
                body.put(appServerBody_AppVersion, MainActivity.appVersionName);
                body.put(appServerBody_RequestMessageKey, appServerBody_RequestMessageValue_GetQRcode);
                body.put(appServerBody_QRcodeKey, BLE_Con_Data_QR_Code);

                Object[] rtnValue = https_request(body);
                resCode = (int) rtnValue[0];
                resMsg = (String) rtnValue[1];

                JSONObject jsonObject = new JSONObject(resMsg);
                BLE_Con_Data_Device_ID = jsonObject.getString("device_id");
                BLE_Con_Data_Auth_Key = jsonObject.getString("ble_auth_key");
            } catch (Exception e) {
                Log.e("Task_GetBLEData", e.toString());
                return null;
            }

            // BLE
            scanLeDeivce(true);
            int exitTime;
            try {
                for (int k = 0; k < 2; k++) {
                    exitTime = 1;
                    while (mConnectionState == STATE_SCANNING) {
                        Thread.sleep(1000);
                        Log.v("Progress Scanning", "Time = " + exitTime);
                        publishProgress("잠금장치를 찾고 있습니다: ", Integer.toString(exitTime));

                        if (exitTime++ > 10) {

                            break;
                        }
                    }

                    if (mConnectionState == STATE_SCANNING) {
                        mBluetoothErrorCode = ERROR_CODE_DEVICE_NOT_FOUND;
                        return null;
                    }


                    exitTime = 1;
                    while (mConnectionState != STATE_SERVICE_FOUND) {
                        Thread.sleep(1000);
                        Log.v("Progress Service", "Time = " + exitTime);
                        publishProgress("잠금장치에 연결 중입니다: ", Integer.toString(exitTime));

                        if (exitTime++ > 10) {
                            break;
                        }

                        if (mConnectionState == STATE_SERVICE_NOT_FOUND) {
                            mBluetoothErrorCode = ERROR_CODE_NO_SERVICE;
                            return null;
                        } else if (mConnectionState == STATE_DISCONNECTED) {
                            mBluetoothErrorCode = ERROR_CODE_GATT_DISCONNECTED;

                            scanLeDeivce(false);
                            bluetooth_initialize();
                            scanLeDeivce(true);

                            //continue;
                        }
                    }
                }

                if (mConnectionState == STATE_DISCONNECTED) {
                    mBluetoothErrorCode = ERROR_CODE_GATT_DISCONNECTED;
                    return null;
                }

                publishProgress("명령을 전송 중입니다.", "");
                Thread.sleep(100);
                if (!bluetooth_data_send(BLE_Con_Data_Auth_Key)) {
                    mBluetoothErrorCode = ERROR_CODE_WRITE_FAIL;
                    return null;
                }
                for(int i = 0; i < 20; i++) {
                    Thread.sleep(100);
                    if (notification_string != null) {
                        if (notification_string.contains("P_OK")) {
                            notification_string = null;
                            break;
                        }
                    }
                }

                Thread.sleep(100);
                String command = "&lstate";
                switch (mCommandMode) {
                    case COMMAND_MODE_UNLOCK:
                        command = "&fopen";
                        break;

                    case COMMAND_MODE_RETURN:
                        command = "&lstate";
                        break;
                }
                Log.e("command",command);
                if (!bluetooth_data_send(command)) {
                    mBluetoothErrorCode = ERROR_CODE_WRITE_FAIL;
                    return null;
                }
                for(int i = 0; i < 5; i++) {
                    Thread.sleep(1000);
                    if (notification_string != null) {
                        Thread.sleep(300);
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Log.e("Task_GetBLEData", e.toString());
                this.cancel(true);
                return null;
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(String ... progress) {
            pDialog.setMessage(progress[0] + " " + progress[1]);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            pDialog.dismiss();
        }

        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            pDialog.dismiss();

            Log.e("notification", Integer.toString(mBluetoothErrorCode));

            if (mBluetoothErrorCode == ERROR_CODE_NO_ERROR) {
                if (notification_string != null) {
                    if (mCommandMode == COMMAND_MODE_UNLOCK) {
                        Log.e("notification_2",notification_string);
                        if (notification_string.contains("L_OP_OK")) {
                            responseDialog.setMessage("열렸습니다.");
                            MainActivity_TextView_ReturnMsg.setText("OPEN");
                            MainActivity_TextView_Time.setVisibility(View.VISIBLE);
                            MainActivity_TextView_Time.setText(getTime());
                        } else if (notification_string.contains("L_OP_FAIL")) {
                            responseDialog.setMessage("잠금 해제에 문제가 있습니다.");
                        }
                    } else {
                        if (notification_string.contains("L_ST_OP")) {
                            responseDialog.setMessage("잠금장치가 열려있습니다. 닫고 다시 시도 하세요.");
                            AlertDialog alert = responseDialog.create();
                            alert.show();
                        } else if (notification_string.contains("L_ST_CL")) {
                            MainActivity_TextView_ReturnMsg.setText("CLOSE");
                            MainActivity_TextView_Time.setText(getTime());
                            BLE_Con_Data_QR_Code = null;
                        } else {
                            responseDialog.setMessage("상태 확인에 문제가 있습니다.");
                            AlertDialog alert = responseDialog.create();
                            alert.show();
                        }
                    }
                } else {
                    responseDialog.setMessage("재시도가 필요합니다.");
                }
            } else if (mBluetoothErrorCode == ERROR_CODE_DEVICE_NOT_FOUND) {
                responseDialog.setMessage("단말기를 찾을 수 없습니다.");
            } else if (mBluetoothErrorCode == ERROR_CODE_GATT_DISCONNECTED) {
                responseDialog.setMessage("연결이 해제되었습니다.");
            } else if (mBluetoothErrorCode == ERROR_CODE_NO_SERVICE) {
                responseDialog.setMessage("서비스가 없습니다.");
            } else if (mBluetoothErrorCode == ERROR_CODE_WRITE_FAIL) {
                responseDialog.setMessage("명령어 전송을 실패하였습니다.");
            } else if (mBluetoothErrorCode == ERROR_CODE_OTHER) {
                responseDialog.setMessage("알 수 없는 오류 #1");
            } else {
                responseDialog.setMessage("알 수 없는 오류 #2");
            }

            if (mCommandMode == COMMAND_MODE_UNLOCK) {
                AlertDialog alert = responseDialog.create();
                alert.show();
            }

            bluetooth_disconnect();
        }
    }
}