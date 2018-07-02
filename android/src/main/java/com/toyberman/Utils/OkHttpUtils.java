package com.toyberman.Utils;

import android.content.Context;
import android.net.Uri;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.toyberman.BuildConfig;

import org.json.JSONException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

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
    private static String content_type = "application/json; charset=utf-8";
    public static MediaType mediaType = MediaType.parse(content_type);

    public static OkHttpClient buildOkHttpClient(CookieJar cookieJar, String hostname, ReadableArray certs) {
        if (client == null) {
            //add logging interceptor
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            CertificatePinner.Builder certificatePinnerBuilder = new CertificatePinner.Builder();
            //get domain
            int slashslash = hostname.indexOf("//") + 2;
            String domain = hostname.substring(slashslash, hostname.indexOf('/', slashslash));
            //add all keys to the certficates pinner
            for (int i = 0; i < certs.size(); i++) {
                certificatePinnerBuilder.add(domain, certs.getString(i));
            }

            CertificatePinner certificatePinner = certificatePinnerBuilder.build();

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

            if (BuildConfig.DEBUG) {
                clientBuilder.addInterceptor(logging);
            }

            client = clientBuilder
                    .cookieJar(cookieJar)
                    .certificatePinner(certificatePinner)
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
