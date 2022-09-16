package com.example.paxdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.gson.Gson;
import com.pax.poslink.CommSetting;
import com.pax.poslink.PaymentRequest;
import com.pax.poslink.PaymentResponse;
import com.pax.poslink.PosLink;
import com.pax.poslink.ProcessTransResult;
import com.pax.poslink.poslink.POSLinkCreator;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        scanDevices();
        findViewById(R.id.connectpax).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanDevices();
            }
        });
    }

    private final int REQUEST_BT_ENABLE = 1;
    private final int REQUEST_BT_DISCOVER = 2;
    private void scanDevices() {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available!", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent;
            if (!btAdapter.isEnabled()) {
                intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivityForResult(intent, REQUEST_BT_ENABLE);
            } else {
                intent = new Intent(this, BluetoothDeviceListActivity.class);
                startActivityForResult(intent, REQUEST_BT_DISCOVER);
            }
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            case REQUEST_BT_ENABLE:
                if (resultCode == Activity.RESULT_OK) {
                    Intent intent = new Intent(this, BluetoothDeviceListActivity.class);
                    startActivityForResult(intent, REQUEST_BT_DISCOVER);
                }
                break;

            case REQUEST_BT_DISCOVER: // bt scan
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(BluetoothDeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    Toast.makeText(this, address, Toast.LENGTH_SHORT).show();
                    setupPayment(address);
                }

                break;
        }
    }
    private void setupPayment(String macAddress){
        CommonObjects.showProgress(this,"Connecting to PAX");
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        CommSetting commset = new CommSetting();
        commset.setType(CommSetting.BT);
        if(!macAddress.isEmpty()) {
            commset.setMacAddr(macAddress);
        }
//
        commset.setTimeOut("60000");
        commset.setDestPort("10009");
        commset.setBaudRate("9600");
        commset.setSerialPort("COM1");
        commset.setEnableProxy(false);
        PosLink poslink = POSLinkCreator.createPoslink(this);//new PosLink(this);
        PaymentRequest requet = new PaymentRequest();
        poslink.SetCommSetting(commset);
//
//// Set the TenderType and TransType for the request first so that POS knows which part of data of request should be used.
        requet.TenderType = requet.ParseTenderType("CREDIT");
        requet.TransType = requet.ParseTransType("SALE");
        // Your unique ID for this transaction
        requet.ECRRefNum = "1";
//
////Optional fields.
        requet.Amount = "100";
        poslink.SetCommSetting(commset);
//// Assign the request instance to poslink instance's PamentRequest field
        poslink.PaymentRequest = requet;
        new Thread(new Runnable() {
            @Override
            public void run() {
                ProcessTransResult ptr = poslink.ProcessTrans();
//        BroadPOSCommunicator.getInstance(this).startListeningService(new BroadPOSCommunicator.StartListenerCallBack() {
//            @Override
//            public void onSuccess() {
//                UIUtil.showToast(getActivity(), "Success", Toast.LENGTH_SHORT);
//            }
//
//            @Override
//            public void onFail(String msg) {
//                UIUtil.showToast(getActivity(), msg, Toast.LENGTH_SHORT);
//            }
//        });
                if(ptr.Code == ProcessTransResult.ProcessTransResultCode.OK) {
                    /**
                     * When the transResult.Code is OK, then the response has already been
                     * assigned to poslink instance's PaymentResponse field automatically,
                     * what you only need to do is get the response from the field
                     */
                    PaymentResponse response = poslink.PaymentResponse;

                    Log.e("Tag",response.AuthCode);
                    Message message = new Message();
                    message.obj = response;
                    message.what=1;
                    handler.sendMessage(message);
                    // Todo assign the response's value to your UI
                } else if (ptr.Code == ProcessTransResult.ProcessTransResultCode.TimeOut) {
                    String errorMsg = ptr.Msg;
                    Log.e("Tag",errorMsg);
                    Message message = new Message();
                    message.obj = errorMsg;
                    message.what=2;
                    handler.sendMessage(message);
                    // Todo show error msg on your UI
                } else {
                    String errorMsg = ptr.Msg;
                    Log.e("Tag",errorMsg);
                    Message message = new Message();
                    message.obj = errorMsg;
                    message.what=2;
                    handler.sendMessage(message);
                    // Todo show error msg on your UI
                }
            }
        }).start();


    }
    Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            CommonObjects.hideProgress();
            if (msg.what == 1) {
                PaymentResponse paymentResponse = (PaymentResponse) msg.obj;
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(new Gson().toJson(paymentResponse))
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
                Toast.makeText(MainActivity.this, "Payment Successfyll!", Toast.LENGTH_SHORT).show();
            }
            if (msg.what == 1) {
                PaymentResponse paymentResponse = (PaymentResponse) msg.obj;
                Toast.makeText(MainActivity.this, "Payment Successfyll!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, msg.obj.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    };

}