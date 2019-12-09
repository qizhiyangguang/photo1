package com.example.filexiazaidemo;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.http.GET;

public interface ApiService {

    public String Url = "http://cdn.banmi.com/banmiapp/";

    @GET("apk/banmi_330.apk")
    Observable<ResponseBody> download();
}
