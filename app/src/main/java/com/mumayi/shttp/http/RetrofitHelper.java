package com.mumayi.shttp.http;

import android.os.Build;

import com.google.gson.Gson;
import com.mumayi.shttp.MyApplication;
import com.mumayi.shttp.util.CommonUtil;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by gh on 2017/6/5.
 */
public class RetrofitHelper {
    private static OkHttpClient mOkHttpClient;

    static {
        initOkttpClient();
    }

//    public static HomeService getHomeAPI() {
//        return createApi(HomeService.class, Constants.HOMEURL);
//    }


    /**
     * 根据传入的baseUrl，和api创建retrofit
     *
     * @param clazz
     * @param baseUrl
     * @param <T>
     * @return
     */
    private static <T> T createApi(Class<T> clazz, String baseUrl) {

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(initOkttpClient())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(new Gson()))
                .build();

        return retrofit.create(clazz);

    }

    /**
     * 初始化okhttpClient,设置缓存，超时时间，设置打印日志，head拦截器
     */
    public static OkHttpClient initOkttpClient() {
        // log打印拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
//                if (LogUtil.isLog())
//                    LogUtil.s("  OkHttp网络请求日志：" + message);
                System.out.println("  OkHttp网络请求日志：" + message);
            }
        }).setLevel(HttpLoggingInterceptor.Level.BODY);

        HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor();
        logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        if (mOkHttpClient == null) {

            if (mOkHttpClient == null) {
                //设置缓存
                Cache cache = new Cache(new File(MyApplication.getInstance()
                        .getCacheDir(), "HttpCache"), 1024 * 1024 * 10);
                mOkHttpClient = new OkHttpClient.Builder()
                        .cache(cache)
//                            .addInterceptor(logInterceptor)
                        .addNetworkInterceptor(new CacheInterceptor())
                        .retryOnConnectionFailure(true)
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(20, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .addInterceptor(new UserAgentInterceptor())
                        .addInterceptor(loggingInterceptor)
                        .build();
            }
        }
        return mOkHttpClient;
    }

    /**
     * 为okhttp添加缓存，这里考虑服务器不支持缓存，从而让okhttp支持缓存
     */
    private static class CacheInterceptor implements Interceptor {

        @Override
        public Response intercept(Chain chain) throws IOException {
            //有网络时，缓存超时时间为一个小时
            int maxAge = 60 * 60;
            //无网络时，设置超时为一天
            int maxStale = 60 * 60 * 24;
            Request request = chain.request();
            if (CommonUtil.isNetworkAvailable(MyApplication.getInstance())) {
                //有网络时，只从网络获取
                request = request.newBuilder()
                        .cacheControl(CacheControl.FORCE_NETWORK)
                        .build();
            } else {
                //无网络时只从缓存中获取
                request = request.newBuilder()
                        .cacheControl(CacheControl.FORCE_CACHE)
                        .build();
            }
            Response response = chain.proceed(request);
            if (CommonUtil.isNetworkAvailable(MyApplication.getInstance())) {
                response = response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "public,max-aget =" + maxAge)
                        .build();
            } else {
                response = response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "punlic,only-if-cached,max-stale" + maxStale)
                        .build();
            }
            return response;
        }
    }

    /**
     * 添加header拦截器
     */
    private static class UserAgentInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request requestWithUserAgent = originalRequest.newBuilder()
                    .removeHeader("User-Agent")
                    .addHeader("User-Agent", Build.MODEL)
                    .build();
            return chain.proceed(requestWithUserAgent);
        }
    }
}
