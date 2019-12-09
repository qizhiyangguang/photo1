package com.example.filexiazaidemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //http://cdn.banmi.com/banmiapp/apk/banmi_330.apk
    /**
     * ok文件下载
     */
    private Button mBt1;
    /**
     * retrofit文件下载
     */
    private Button mBt2;
    /**
     * http文件下载
     */
    private Button mBt3;
    private File sd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initSD();
    }

    private void initSD() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            openSd();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openSd();
        }
    }

    private void openSd() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            sd = Environment.getExternalStorageDirectory();
        }
    }

    private void initView() {
        mBt1 = (Button) findViewById(R.id.bt1);
        mBt1.setOnClickListener(this);
        mBt2 = (Button) findViewById(R.id.bt2);
        mBt2.setOnClickListener(this);
        mBt3 = (Button) findViewById(R.id.bt3);
        mBt3.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.bt1:
                ok();
                break;
            case R.id.bt2:
                retrofit();
                break;
            case R.id.bt3:
                http();
                break;
        }
    }

    private void http() {
        new Thread() {
            @Override
            public void run() {
                super.run();

                try {
                    URL url = new URL("http://cdn.banmi.com/banmiapp/apk/banmi_330.apk");
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();

                    int responseCode = con.getResponseCode();
                    if (responseCode == 200) {
                        InputStream inputStream = con.getInputStream();
                        int max = con.getContentLength();

                        saveFile(inputStream, sd + "/" + "abc789.apk", max);
                    }

                } catch (MalformedURLException e) {
                    e.printStackTrace();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private void retrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiService.Url)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();

        ApiService apiService = retrofit.create(ApiService.class);

        Observable<ResponseBody> download = apiService.download();

        download.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Observer<ResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(ResponseBody responseBody) {
                        InputStream inputStream = responseBody.byteStream();
                        long max = responseBody.contentLength();

                        saveFile(inputStream, sd + "/" + "abc456.apk", max);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e("tag", "onError: " + e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void ok() {
        OkHttpClient client = new OkHttpClient.Builder()
                .build();

        Request request = new Request.Builder()
                .url("http://cdn.banmi.com/banmiapp/apk/banmi_330.apk")
                .get()
                .build();

        Call call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("tag", "onFailure: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                ResponseBody body = response.body();
                InputStream inputStream = body.byteStream();
                long max = body.contentLength();

                saveFile(inputStream, sd + "/" + "abc123.apk", max);
            }


        });
    }

    private void saveFile(InputStream inputStream, final String string, long max) {
        //读写的进度
        long count = 0;
        try {
            FileOutputStream outputStream = new FileOutputStream(new File(string));

            byte[] bys = new byte[1024 * 10];
            int length = -1;

            while ((length = inputStream.read(bys)) != -1) {
                outputStream.write(bys, 0, length);

                count += length;

                Log.d("tag", "progress: " + count + "  max:" + max);
            }

            inputStream.close();
            outputStream.close();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "下载完成", Toast.LENGTH_SHORT).show();

                    InstallUtil.installApk(MainActivity.this, string);
                }
            });
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
