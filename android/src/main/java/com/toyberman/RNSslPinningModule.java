package com.toyberman;

import android.os.Build;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.network.ForwardingCookieHandler;
import com.toyberman.Utils.OkHttpUtils;

import org.json.JSONException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RNSslPinningModule extends ReactContextBaseJavaModule {


    private static final String OPT_SSL_PINNING_KEY = "sslPinning";
    private static final String DISABLE_ALL_SECURITY = "disableAllSecurity";
    private static final String RESPONSE_TYPE = "responseType";
    private static final String KEY_NOT_ADDED_ERROR = "sslPinning key was not added";

    private final ReactApplicationContext reactContext;
    private final HashMap<String, List<Cookie>> cookieStore;
    private CookieJar cookieJar = null;
    private ForwardingCookieHandler cookieHandler;
    private OkHttpClient client;

    public RNSslPinningModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        cookieStore = new HashMap<>();
        cookieHandler = new ForwardingCookieHandler(reactContext);
        cookieJar = new CookieJar() {

            @Override
            public synchronized void saveFromResponse(HttpUrl url, List<Cookie> unmodifiableCookieList) {
                for (Cookie cookie : unmodifiableCookieList) {
                    setCookie(url, cookie);
                }
            }

            @Override
            public synchronized List<Cookie> loadForRequest(HttpUrl url) {
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
                try {
                    putCookie(url, cookieListForUrl, cookie);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            private void putCookie(HttpUrl url, List<Cookie> storedCookieList, Cookie newCookie) throws URISyntaxException, IOException {

                Cookie oldCookie = null;
                Map<String, List<String>> cookieMap = new HashMap<>();

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

                cookieMap.put("Set-cookie", Collections.singletonList(newCookie.toString()));
                cookieHandler.put(url.uri(), cookieMap);
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

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    map.putString(cookie.name(), cookie.value());
                }
            }

            promise.resolve(map);
        } catch (Exception e) {
            promise.reject(e);
        }
    }


    @ReactMethod
    public void removeCookieByName(String cookieName, final Promise promise) {
        List<Cookie> cookies = null;

        for (String domain : cookieStore.keySet()) {
            List<Cookie> newCookiesList = new ArrayList<>();

            cookies = cookieStore.get(domain);
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (!cookie.name().equals(cookieName)) {
                        newCookiesList.add(cookie);
                    }
                }
                cookieStore.put(domain, newCookiesList);
            }
        }

        promise.resolve(null);
    }

    @ReactMethod
    public void fetch(String hostname, final ReadableMap options, final Callback callback) {

        final WritableMap response = Arguments.createMap();
        String domainName;
        try {
            domainName = getDomainName(hostname);
        } catch (URISyntaxException e) {
            domainName = hostname;
        }
        if (options.hasKey(DISABLE_ALL_SECURITY) && options.getBoolean(DISABLE_ALL_SECURITY)) {
            client = OkHttpUtils.buildDefaultOHttpClient(cookieJar, domainName, options);
        }
        // With ssl pinning
        else if (options.hasKey(OPT_SSL_PINNING_KEY)) {
            if (options.getMap(OPT_SSL_PINNING_KEY).hasKey("certs")) {
                ReadableArray certs = options.getMap(OPT_SSL_PINNING_KEY).getArray("certs");
                if (certs != null && certs.size() == 0) {
                    throw new RuntimeException("certs array is empty");
                }
                client = OkHttpUtils.buildOkHttpClient(cookieJar, domainName, certs, options);
            } else {
                callback.invoke(new Throwable("key certs was not found"), null);
            }
        } else {
            //no ssl pinning
            callback.invoke(new Throwable(KEY_NOT_ADDED_ERROR), null);
            return;
        }

        try {
            Request request = OkHttpUtils.buildRequest(this.reactContext, options, hostname);

            client.newCall(request).enqueue(new okhttp3.Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.invoke(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response okHttpResponse) throws IOException {
                    byte[] bytes = okHttpResponse.body().bytes();
                    String stringResponse = new String(bytes, "UTF-8");
                    String responseType = "";

                    //build response headers map
                    WritableMap headers = buildResponseHeaders(okHttpResponse);
                    //set response status code
                    response.putInt("status", okHttpResponse.code());
                    if (options.hasKey(RESPONSE_TYPE)) {
                        responseType = options.getString(RESPONSE_TYPE);
                    }
                    switch (responseType) {
                        case "base64":
                            String base64;
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
                            } else {
                                base64 = Base64.getEncoder().encodeToString(bytes);
                            }
                            response.putString("data", base64);
                            break;
                        default:
                            response.putString("bodyString", stringResponse);
                            break;
                    }
                    response.putMap("headers", headers);

                    if (okHttpResponse.isSuccessful()) {
                        callback.invoke(null, response);
                    } else {
                        callback.invoke(response, null);
                    }
                }
            });


        } catch (JSONException e) {
            callback.invoke(e, null);
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
