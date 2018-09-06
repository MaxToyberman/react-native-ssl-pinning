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
import com.facebook.react.bridge.WritableNativeMap;
import com.toyberman.Utils.OkHttpUtils;

import org.json.JSONException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RNSslPinningModule extends ReactContextBaseJavaModule {


    private static final String OPT_SSL_PINNING_KEY = "sslPinning";

    private final ReactApplicationContext reactContext;
    private final HashMap<String, List<Cookie>> cookieStore;
    private CookieJar cookieJar = null;
    private OkHttpClient client;

    public RNSslPinningModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        cookieStore = new HashMap<>();
        cookieJar = new CookieJar() {


            @Override
            public void saveFromResponse(HttpUrl url, List<Cookie> unmodifiableCookieList) {

                for (Cookie cookie : unmodifiableCookieList) {
                    setCookie(url, cookie);
                }

            }

            @Override
            public List<Cookie> loadForRequest(HttpUrl url) {
                List<Cookie> cookies = cookieStore.get(url.host());
                return cookies != null ? cookies : new ArrayList<Cookie>();
            }


            public void setCookie(HttpUrl url, Cookie cookie) {

                final String host = url.host();

                List<Cookie> cookieListForUrl = cookieStore.get(host);
                if (cookieListForUrl == null) {
                    cookieListForUrl = new ArrayList<Cookie>();
                    cookieStore.put(host, cookieListForUrl);
                }
                putCookie(cookieListForUrl, cookie);

            }

            private void putCookie(List<Cookie> storedCookieList, Cookie newCookie) {

                Cookie oldCookie = null;
                for (Cookie storedCookie : storedCookieList) {

                    // create key for comparison
                    final String oldCookieKey = storedCookie.name() + storedCookie.path();
                    final String newCookieKey = newCookie.name() + newCookie.path();

                    if (oldCookieKey.equals(newCookieKey)) {
                        oldCookie = storedCookie;
                        break;
                    }
                }
                if (oldCookie != null) {
                    storedCookieList.remove(oldCookie);
                }
                storedCookieList.add(newCookie);
            }
        };

    }

    public static String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    @ReactMethod
    public void getCookies(String domain, final Promise promise) {
        try {
            WritableMap map = new WritableNativeMap();

            List<Cookie> cookies = cookieStore.get(getDomainName(domain));
            for (Cookie cookie : cookies) {
                map.putString(cookie.name(), cookie.value());
            }

            promise.resolve(map);
        } catch (Exception e) {
            promise.reject(e);
        }
    }

    @ReactMethod
    public void fetch(String hostname, ReadableMap options, Promise promise) {

        WritableMap response = Arguments.createMap();
        // With ssl pinning
        if (options.hasKey(OPT_SSL_PINNING_KEY)) {
            if (options.getMap(OPT_SSL_PINNING_KEY).hasKey("certs")) {
                ReadableArray certs = options.getMap(OPT_SSL_PINNING_KEY).getArray("certs");
                if (certs.size() == 0) {
                    throw new RuntimeException("certs array is empty");
                }
                if (certs != null) {
                    client = OkHttpUtils.buildOkHttpClient(cookieJar, hostname, certs, options);
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
            Request request = OkHttpUtils.buildRequest(this.reactContext, options, hostname);

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