package com.toyberman;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

public class RNSslPinningModule extends NativeRNSslPinningSpec {

    private final RNSslPinningImpl sslImpl;

    public RNSslPinningModule(ReactApplicationContext reactContext) {
        super(reactContext);
        sslImpl = new RNSslPinningImpl(reactContext);
    }

    @ReactMethod
    public void getCookies(String domain, final Promise promise) {
        sslImpl.getCookies(domain, promise);
    }

    @ReactMethod
    public void removeCookieByName(String cookieName, final Promise promise) {
        sslImpl.removeCookieByName(cookieName, promise);
    }

    @ReactMethod
    public void fetch(String hostname, final ReadableMap options, final Callback callback) {
        sslImpl.fetch(hostname, options, callback);
    }

    @Override
    public String getName() {
        return RNSslPinningImpl.NAME;
    }

}
