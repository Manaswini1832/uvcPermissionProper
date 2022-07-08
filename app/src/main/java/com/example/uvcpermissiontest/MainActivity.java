package com.example.uvcpermissiontest;

import static java.lang.Math.min;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private View mainFrame = null;
    private ScrollView mLogScrollView = null;
    private TextView logTv = null;

    private static final boolean DEBUG = true;
    private  static final String TAG = "CAMERA_APP_LOGS";
    private static final String ACTION_USB_PERMISSION = "ACTION_USB_PERMISSION";
    private static final int REQ_PERMISSION_USB = 16;
    private static final int REQ_PERMISSION_CAMERA = 18;
//    private static final String packageName = "com.example.uvcpermissiontest";

    private Button btnGet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(DEBUG) Log.v(TAG, "onCreate:");
        setContentView(R.layout.activity_main);

        btnGet = findViewById(R.id.btnGet);

        this.setTitle("targetSDK" + getApplicationContext().getApplicationInfo().targetSdkVersion);
        mainFrame = findViewById(R.id.main_frame);
        mLogScrollView = findViewById(R.id.log_scrollview);
        logTv = findViewById(R.id.log_textview);

        btnGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanAttachedDevice();
            }
        });

        // output information to screen
        String msg = "targetSDKVersion " + getApplicationContext().getApplicationInfo().targetSdkVersion;;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            msg = msg + "\n    minSDKVersion " + getApplicationContext().getApplicationInfo().minSdkVersion;;
        }
        msg = msg + "\n    SDK_INT=" + Build.VERSION.SDK_INT + "\n" +
                "    BOARD=" + Build.BOARD + "\n" +
                "    BOOTLOADER=" + Build.BOOTLOADER + "\n" +
                "    BRAND=" + Build.BRAND + "\n" +
                "    DEVICE=" + Build.DEVICE + "\n" +
                "    DISPLAY=" + Build.DISPLAY + "\n" +
                "    HARDWARE=" + Build.HARDWARE + "\n" +
                "    ID=" + Build.ID + "\n" +
                "    MANUFACTURER=" + Build.MANUFACTURER + "\n" +
                "    PRODUCT=" + Build.PRODUCT + "\n" +
                "    TAGS=" + Build.TAGS + "\n" +
                "    VERSION.MODEL=" + Build.MODEL + "\n";
        log("Info", msg);

        // check CAMERA permission
        boolean hasPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android 9 and later needs CAMERA permission to access UVC devices
            if (hasPermission) {
                log("CAMERA permission", "already granted");
            } else {
                log("CAMERA permission", "requesting permission starting here");
                requestCameraPermission();
            }
        } else {
            log("CAMERA permission", "before Android 9,CAMERA permission=" + hasPermission);
        }
        // Register BroadcastReceiver to receive USB related events
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mBroadcastReceiver, filter);
        log("BroadcastReceiver", "register");
//        if (intent != null) {
//            MainActivity.this.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    handleintent(intent);
//                }
//            });
//        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (DEBUG) Log.v(TAG, "onNewIntent: " + intent);
        if (intent != null) {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    handleintent(intent);
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        if(DEBUG) Log.v(TAG, "onDestroy:");
        unregisterReceiver(mBroadcastReceiver);
        log("BroadcastReceiver", "unregister");
        super.onDestroy();
    }

    //--------------------------------------------------------------------------------
    /**
     * When received result of permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        int n = min(permissions.length, grantResults.length);
        for (int i = 0; i < n; i++) {
            handlePermissionResult(permissions[i],
                    grantResults[i] == PackageManager.PERMISSION_GRANTED);
        }
    }

    private void handlePermissionResult(String permission, Boolean result) {
        if (result) {
//            if (permission == android.Manifest.permission.CAMERA) {
//                //TODO : Even if result = true, it is still not coming into this second if statement for some reason. DEBUG THIS. For now I'm removing this loop
//                log("handle permission result", "Inside second if");
//                log("CAMERA permission", "granted");
//                Snackbar.make(mainFrame,
//                        R.string.camera_permission_granted, Snackbar.LENGTH_SHORT
//				).show();
//                scanAttachedDevice();
//            }
            log("CAMERA permission", "granted");
            Snackbar.make(mainFrame,
                    R.string.camera_permission_granted, Snackbar.LENGTH_SHORT
            ).show();
            scanAttachedDevice();
        } else {
            log("handle permission result", "Inside else");
            log("CAMERA permission", "denied");
            Snackbar.make(mainFrame,
                    R.string.camera_permission_denied, Snackbar.LENGTH_SHORT
			).show();
        }
    }

    //--------------------------------------------------------------------------------
    /**
     * request CAMERA permission
     * Android 9 and later needs CAMERA permission to access UVC devices
     */
    private void requestCameraPermission() {
        if (DEBUG) Log.v(TAG, "requestCameraPermission:");
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            if (mainFrame != null) {
                Snackbar snackbar = Snackbar
                        .make(mainFrame, R.string.camera_access_required, Snackbar.LENGTH_INDEFINITE)
                        .setAction("YES", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //Request permission
                                log("CAMERA permission", "request");
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQ_PERMISSION_CAMERA);
                            }
                        });
                snackbar.show();
            }
        } else {
            Snackbar.make(mainFrame, R.string.camera_unavailable, Snackbar.LENGTH_SHORT).show();
            log("CAMERA permission", "request");
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQ_PERMISSION_CAMERA);
        }
    }

    //--------------------------------------------------------------------------------
    /**
     * BroadcastReceiver to receive USB related events
     */
    /**
     * BroadcastReceiver to receive USB related events
     */
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                handleintent(intent);
            }
        }
    };

    /**
     * Handle received Intent
     */
    private void handleintent(Intent intent) {
        if(DEBUG) Log.v(TAG, "handleIntent : " + intent);
        switch(intent.getAction()) {
            case UsbManager.ACTION_USB_DEVICE_ATTACHED :
				handleActionOnAttachDevice(intent);
                break;
            case UsbManager.ACTION_USB_DEVICE_DETACHED :
				handleActionOnDetachDevice(intent);
                break;
            case ACTION_USB_PERMISSION :
                handleActionUsbPermission(intent);
                break;
            case Intent.ACTION_MAIN :
				scanAttachedDevice();// app launched by user
                break;
            default :
                log("Unknown intent", "action=" + intent.getAction());
                break;
        }
    }

    /**
     * when app received attach event of USB device
     */
    private void handleActionOnAttachDevice(Intent intent) {
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        boolean hasPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
        if (device != null) {
            hasPermission = hasPermission || manager.hasPermission(device);
            log("USB_DEVICE_ATTACHED", deviceName(device) + "hasPermission="+ hasPermission + "\n");
            if (!hasPermission) {
                requestUsbPermission(manager, device);
            } else {
                log("USB permission", deviceName(device) + "already has permission:\n");
            }
        } else {
            log("USB_DEVICE_ATTACHED", "device is null");
        }
    }

    /**
     * when app received detach event of USB device
     */
    private void handleActionOnDetachDevice(Intent intent) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        log("USB_DEVICE_DETACHED", deviceName(device));
    }

    /**
     * when app received the result of requesting USB access permission.
     */
    private void handleActionUsbPermission(Intent intent) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        boolean hasPermission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
        log("Result", deviceName(device) + " has permission=" + hasPermission + "\n");
    }

//--------------------------------------------------------------------------------
    /**
     * scan attached usb devices and request permission for first found device that has no permission
     */
    private void scanAttachedDevice() {
        if (DEBUG) Log.v(TAG, "scanAttachedDevice:");
        log("SCAN", "start");
        UsbManager manager = (UsbManager)getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        UsbDevice device = null;
        while (deviceIterator.hasNext()) {
            device = deviceIterator.next();
            log("Device_detected", deviceName(device));
            if (isUVC(device)) {
                if (!manager.hasPermission(device)) {
                    requestUsbPermission(manager, device);
                    break;
                } else {
                    log("USB permission", "already has permission:\n" + deviceName(device));
                }
            }

        }
        log("SCAN", "finished");
    }

    /**
     * request USB permission for specific device
     */
    private void requestUsbPermission(UsbManager manager, UsbDevice device) {
        log("USB permission", "requesting for " + deviceName(device));
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PendingIntent permissionIntent = PendingIntent.getBroadcast(MainActivity.this, REQ_PERMISSION_USB, new Intent(ACTION_USB_PERMISSION), 0);
                manager.requestPermission(device, permissionIntent);
            }
        });
    }

    /**
     * check whether the specific device or one of its interfaces is VIDEO class
     */
    private boolean isUVC(UsbDevice device) {
        boolean result = false;
        if (device != null) {
            if (device.getDeviceClass() == UsbConstants.USB_CLASS_VIDEO) {
                result = true;
            } else {
                for(int i = 0; i < device.getInterfaceCount(); i++){
                    UsbInterface iface = device.getInterface(i);
                    if (iface.getInterfaceClass() == UsbConstants.USB_CLASS_VIDEO) {
                        result = true;
                        break;
                    }
                }
            }
        }
        Toast.makeText(this, deviceName(device) + " UVC : " + result, Toast.LENGTH_SHORT).show();
        return result;
    }

    /**
     * get device name of specific UsbDevice
     * return productName if it is available else return deviceName
     */
    private String deviceName(UsbDevice device){
        String result = "device is null";
        if (device != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if(!TextUtils.isEmpty(device.getProductName())){
                    result = device.getProductName();
                }else{
                    result = device.getDeviceName();
                }
            } else {
                result = device.getDeviceName();
            }
        }
        return result;
    }

    //--------------------------------------------------------------------------------

    /**
     * add message to TextView
     */
    private void log(String tag, String msg) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logTv.append(tag + " : " + msg + "\n");
                mLogScrollView.scrollTo(0, logTv.getBottom());
            }
        });

    }
}