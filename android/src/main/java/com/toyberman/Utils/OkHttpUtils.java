package com.toyberman.Utils;

import android.content.Context;
import android.net.Uri;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.toyberman.BuildConfig;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import okhttp3.CertificatePinner;
import okhttp3.CookieJar;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by Max Toyberman on 2/11/18.
 */

public class OkHttpUtils {

    private static final String HEADERS_KEY = "headers";
    private static final String BODY_KEY = "body";
    private static final String METHOD_KEY = "method";
    private static final String FILE = "file";
    private static OkHttpClient client = null;
    private static SSLContext sslContext;
    private static String content_type = "application/json; charset=utf-8";
    public static MediaType mediaType = MediaType.parse(content_type);

    public static OkHttpClient buildOkHttpClient(CookieJar cookieJar, String hostname, ReadableArray certs, ReadableMap options) {
        if (client == null) {
            // add logging interceptor
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            
            // SSLFactory
            try {
                sslContext = SSLContext.getInstance("TLS");
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                String keyStoreType = KeyStore.getDefaultType();
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(null, null);

                for (int i = 0; i < certs.size(); i++) {
                    String filename = certs.getString(i);
                    InputStream caInput = new BufferedInputStream(OkHttpUtils.class.getClassLoader().getResourceAsStream("assets/" + filename + ".cer"));
                    Certificate ca;
                    try {
                        ca = cf.generateCertificate(caInput);
                    } finally {
                        caInput.close();
                    }

                    keyStore.setCertificateEntry(filename, ca);
                }

                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keyStore);

                sslContext.init(null, tmf.getTrustManagers(), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

            if (options.hasKey("timeoutInterval")) {
                int timeout = options.getInt("timeoutInterval");
                clientBuilder
                        .readTimeout(timeout, TimeUnit.MILLISECONDS);
            }


            if (BuildConfig.DEBUG) {
                clientBuilder.addInterceptor(logging);
            }

            client = clientBuilder
                    .cookieJar(cookieJar)
                    .sslSocketFactory(sslContext.getSocketFactory())
                    .build();

        }
        return client;
    }

    public static Request buildRequest(Context context, ReadableMap options, String hostname) throws JSONException {

        Request.Builder requestBuilder = new Request.Builder();
        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);

        RequestBody body = null;

        String method = "GET";

        if (options.hasKey(HEADERS_KEY)) {
            setRequestHeaders(options, requestBuilder);
        }

        if (options.hasKey(METHOD_KEY)) {
            method = options.getString(METHOD_KEY);
        }

        if (options.hasKey(BODY_KEY)) {
            method = "POST";
            ReadableType bodyType = options.getType(BODY_KEY);
            switch (bodyType) {
                case String:
                    body = RequestBody.create(mediaType, options.getString(BODY_KEY));
                    break;
                case Array:
                    //get array from body
                    ReadableArray bodyArray = options.getArray(BODY_KEY);
                    for (int i = 0; i < bodyArray.size(); i++) {
                        //get body object at index i
                        ReadableMap bodyPart = bodyArray.getMap(i);
                        ReadableMapKeySetIterator iterator = bodyPart.keySetIterator();
                        //loop over the keys of each object
                        while (iterator.hasNextKey()) {
                            String key = iterator.nextKey();
                            if (key.equals(FILE)) {
                                ReadableMap fileMap = bodyPart.getMap(key);
                                if (fileMap.hasKey("uri")) {
                                    Uri uri = Uri.parse(fileMap.getString("uri"));
                                    InputStream inputstream;
                                    File file = null;
                                    try {
                                        file = new File(context.getCacheDir(), fileMap.getString("fileName"));
                                        inputstream = context.getContentResolver().openInputStream(uri);
                                        Utilities.copyInputStreamToFile(inputstream, file);
                                    } catch (FileNotFoundException e) {
                                        if (BuildConfig.DEBUG) {
                                            e.printStackTrace();
                                        }
                                    }
                                    //add file to body if exists
                                    if (file.exists()) {
                                        final MediaType MEDIA_TYPE = MediaType.parse(fileMap.getString("type"));
                                        multipartBodyBuilder.addFormDataPart("file", fileMap.getString("fileName"), RequestBody.create(MEDIA_TYPE, file));
                                    }
                                }

                            } else {
                                //some param
                                multipartBodyBuilder.addFormDataPart(key, bodyPart.getString(key));
                            }
                        }
                    }
                    body = multipartBodyBuilder.build();
                    break;
            }

        }
        return requestBuilder.url(hostname)
                .method(method, body)
                .build();
    }

    private static void setRequestHeaders(ReadableMap options, Request.Builder requestBuilder) {
        ReadableMap map = options.getMap((HEADERS_KEY));
        //add headers to request
        Utilities.addHeadersFromMap(map, requestBuilder);
        if (map.hasKey("content-type")) {
            content_type = map.getString("content-type");
            mediaType = MediaType.parse(content_type);
        }
    }
}
