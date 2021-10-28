package com.inmo.wififiletransfer;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.inmo.wifitransferfilelib.base.BaseActivity;
import com.inmo.wifitransferfilelib.model.FileTransfer;
import com.inmo.wifitransferfilelib.service.FileReceiverService;

import java.io.File;
import java.util.Locale;

public class FileReceiverActivity extends BaseActivity {

    private FileReceiverService fileReceiverService;

    private ProgressDialog progressDialog;

    private TextView msgTv;

    private static final String TAG = "ReceiverActivity";

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FileReceiverService.MyBinder binder = (FileReceiverService.MyBinder) service;
            fileReceiverService = binder.getService();
            fileReceiverService.setProgressChangListener(progressChangListener);
            if (!fileReceiverService.isRunning()) {
                FileReceiverService.startActionTransfer(FileReceiverActivity.this);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            fileReceiverService = null;
            bindService(FileReceiverService.class, serviceConnection);
        }
    };

    private final FileReceiverService.OnReceiveProgressChangListener progressChangListener = new FileReceiverService.OnReceiveProgressChangListener() {

        private FileTransfer originFileTransfer;

        @Override
        public void onProgressChanged(final FileTransfer fileTransfer, final long totalTime, final int progress, final double instantSpeed, final long instantRemainingTime, final double averageSpeed, final long averageRemainingTime) {
            this.originFileTransfer = fileTransfer;
            runOnUiThread(() -> {
                if (isCreated()) {
                    progressDialog.setTitle("正在接收的文件： " + originFileTransfer.getFileName());
                    if (progress != 100) {
                        progressDialog.setMessage("总的传输时间：" + totalTime + " 秒"
                                + "\n\n" + "瞬时-传输速率：" + (int) instantSpeed + " Kb/s"
                                + "\n" + "瞬时-预估的剩余完成时间：" + instantRemainingTime + " 秒"
                                + "\n\n" + "平均-传输速率：" + (int) averageSpeed + " Kb/s"
                                + "\n" + "平均-预估的剩余完成时间：" + averageRemainingTime + " 秒"
                        );
                    }
                    progressDialog.setProgress(progress);
                    progressDialog.setCancelable(true);
                    progressDialog.show();
                }
            });
        }

        @Override
        public void onStartComputeMD5() {
            runOnUiThread(() -> {
                if (isCreated()) {
                    progressDialog.setTitle("传输结束，正在计算本地文件的MD5码以校验文件完整性");
                    progressDialog.setCancelable(false);
                    progressDialog.show();
                }
            });
        }

        @Override
        public void onTransferSucceed(final FileTransfer fileTransfer) {
            runOnUiThread(() -> {
                if (isCreated()) {
                    progressDialog.setTitle("传输成功");
                    progressDialog.setMessage("文件位置：" + fileTransfer.getFilePath());
                    progressDialog.setCancelable(true);
                    progressDialog.show();
                    Glide.with(FileReceiverActivity.this).load(fileTransfer.getFilePath()).into(iv_image);
                }
            });
        }

        @Override
        public void onTransferFailed(final FileTransfer fileTransfer, final Exception e) {
            runOnUiThread(() -> {
                if (isCreated()) {
                    progressDialog.setTitle("传输失败");
                    progressDialog.setMessage("文件位置：" + fileTransfer.getFilePath()
                            + "\n" + "异常信息：" + e.getMessage());
                    progressDialog.setCancelable(true);
                    progressDialog.show();
                }
            });
        }
    };

    private ImageView iv_image;
    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_receiver);
        initView();
        bindService(FileReceiverService.class, serviceConnection);
    }

    private void initView() {
        setTitle("接收文件");
        msgTv = findViewById(R.id.tv_hint);
        Button createWifiHostBtn = findViewById(R.id.btn_create_wifi_host);
        createWifiHostBtn.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 1);
                } else {
                    //创建热点
                    createWifiHotspot();
                }
            }
        });
        iv_image = findViewById(R.id.iv_image);
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setTitle("正在接收文件");
        progressDialog.setMax(100);
    }

    /**
     * 创建随机热点
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createWifiHotspot() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        wifiManager.startLocalOnlyHotspot(new WifiManager.LocalOnlyHotspotCallback() {
            @Override
            public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation) {
                super.onStarted(reservation);
                Log.e(TAG,"随机热点创建成功，SSID= " + reservation.getWifiConfiguration().SSID + ",pwd = " + reservation.getWifiConfiguration().preSharedKey);
                msgTv.setText("随机热点创建成功，SSID= " + reservation.getWifiConfiguration().SSID + ",pwd = " + reservation.getWifiConfiguration().preSharedKey);
            }

            @Override
            public void onStopped() {
                super.onStopped();
                Log.e(TAG, "createWifiHotspot onStopped");
            }

            @Override
            public void onFailed(int reason) {
                super.onFailed(reason);
                Log.e(TAG, "createWifiHotspot onFailed,reason=" + reason);
            }
        }, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fileReceiverService != null) {
            fileReceiverService.setProgressChangListener(null);
            unbindService(serviceConnection);
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void openFile(String filePath) {
        String ext = filePath.substring(filePath.lastIndexOf('.')).toLowerCase(Locale.US);
        try {
            MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
            String mime = mimeTypeMap.getMimeTypeFromExtension(ext.substring(1));
            mime = TextUtils.isEmpty(mime) ? "" : mime;
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(filePath)), mime);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "文件打开异常：" + e.getMessage());
            showToast("文件打开异常：" + e.getMessage());
        }
    }

}