package com.toyberman;

import android.support.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.toyberman.Utils.OkHttpUtils;

import org.json.JSONException;

import java.io.IOException;
import java.util.Set;

import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RNSslPinningModule extends ReactContextBaseJavaModule {


    private static final String OPT_SSL_PINNING_KEY = "sslPinning";

    private final ReactApplicationContext reactContext;
    private CookieJar cookieJar = null;
    private OkHttpClient client;

    public RNSslPinningModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;

        cookieJar = new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(this.reactContext));
    }

    @ReactMethod
    public void fetch(String hostname, ReadableMap options, Promise promise) {

        WritableMap response = Arguments.createMap();
        // With ssl pinning
        if (options.hasKey(OPT_SSL_PINNING_KEY)) {
            if (options.getMap(OPT_SSL_PINNING_KEY).hasKey("certs")) {
                ReadableArray certs = options.getMap(OPT_SSL_PINNING_KEY).getArray("certs");
                if (certs != null) {
                    client = OkHttpUtils.buildOkHttpClient(cookieJar, hostname, certs);
                }
            } else {
                promise.reject(new Throwable("key certs was not found"));
            }
        } else {
            //no ssl pinning
            promise.reject(new Throwable("sslPinning key was not added"));
            return;
        }

        try {
            Request request = OkHttpUtils.buildRequest(this.reactContext,options, hostname);

            Response okHttpResponse = client.newCall(request).execute();

            if (okHttpResponse.isSuccessful()) {

                String stringResponse = okHttpResponse.body().string();
                //build response headers map
                WritableMap headers = buildResponseHeaders(okHttpResponse);
                //set response status code
                response.putInt("status", okHttpResponse.code());
                response.putString("bodyString", stringResponse);
                response.putMap("headers", headers);

                promise.resolve(response);
            } else {
                promise.reject(Integer.toString(okHttpResponse.code()), okHttpResponse.message());
            }
        } catch (IOException | JSONException e) {
            promise.reject(e);
        }

    }

    @NonNull
    private WritableMap buildResponseHeaders(Response okHttpResponse) {
        Headers responseHeaders = okHttpResponse.headers();
        Set<String> headerNames = responseHeaders.names();
        WritableMap headers = Arguments.createMap();
        for (String header : headerNames) {
            headers.putString(header, responseHeaders.get(header));
        }
        return headers;
    }

    @Override
    public String getName() {
        return "RNSslPinning";
    }

}