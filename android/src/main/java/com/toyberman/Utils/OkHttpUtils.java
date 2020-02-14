package com.toyberman.Utils;

import android.content.Context;
import android.net.Uri;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.toyberman.BuildConfig;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
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
    private static final HashMap<String, OkHttpClient> clientsByDomain = new HashMap<>();
    //    private static OkHttpClient client = null;
    private static SSLContext sslContext;
    private static String content_type = "application/json; charset=utf-8";
    public static MediaType mediaType = MediaType.parse(content_type);

    public static OkHttpClient buildOkHttpClient(CookieJar cookieJar, String domainName, ReadableArray certs, ReadableMap options) {

        OkHttpClient client = null;
        CertificatePinner certificatePinner = null;
        if (!clientsByDomain.containsKey(domainName)) {
            // add logging interceptor
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
            clientBuilder.cookieJar(cookieJar);

            if (options.hasKey("pkPinning") && options.getBoolean("pkPinning") == true) {
                // public key pinning
                certificatePinner = initPublicKeyPinning(certs, domainName);
                clientBuilder.certificatePinner(certificatePinner)
            } else {
                // ssl pinning
                initSSLPinning(certs);
                clientBuilder
                        .sslSocketFactory(sslContext.getSocketFactory())
            }


            if (BuildConfig.DEBUG) {
                clientBuilder.addInterceptor(logging);
            }

            client = clientBuilder
                    .build();


            clientsByDomain.put(domainName, client);
            return client;
        }

        client = clientsByDomain.get(domainName);


        if (options.hasKey("timeoutInterval")) {
            int timeout = options.getInt("timeoutInterval");
            // Copy to customize OkHttp for this request.
            OkHttpClient client2 = client.newBuilder()
                    .readTimeout(timeout, TimeUnit.MILLISECONDS)
                    .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                    .connectTimeout(timeout, TimeUnit.MILLISECONDS).build();
            return client2;
        }


        return client;

    }

    private static CertificatePinner initPublicKeyPinning(initSSLPinning pins, String domain) {


        CertificatePinner.Builder certificatePinnerBuilder = new CertificatePinner.Builder();
        //add all keys to the certficates pinner
        for (int i = 0; i < certs.size(); i++) {
            certificatePinnerBuilder.add(domain, certs.getString(i));
        }

        CertificatePinner certificatePinner = certificatePinnerBuilder.build();

        return certificatePinner

    }

    private static void initSSLPinning(ReadableArray certs) {
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
    }

    public static Request buildRequest(Context context, ReadableMap options, String hostname) throws JSONException {

        Request.Builder requestBuilder = new Request.Builder();
        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        multipartBodyBuilder.setType((MediaType.parse("multipart/form-data")));
        RequestBody body = null;

        String method = "GET";

        if (options.hasKey(HEADERS_KEY)) {
            setRequestHeaders(options, requestBuilder);
        }

        if (options.hasKey(METHOD_KEY)) {
            method = options.getString(METHOD_KEY);
        }

        if (options.hasKey(BODY_KEY)) {

            ReadableType bodyType = options.getType(BODY_KEY);
            switch (bodyType) {
                case String:
                    body = RequestBody.create(mediaType, options.getString(BODY_KEY));
                    break;
                case Map:
                    ReadableMap bodyMap = options.getMap(BODY_KEY);
                    if (bodyMap.hasKey("formData")) {
                        ReadableMap formData = bodyMap.getMap("formData");

                        if (formData.hasKey("_parts")) {
                            ReadableArray parts = formData.getArray("_parts");
                            for (int i = 0; i < parts.size(); i++) {
                                ReadableArray part = parts.getArray(i);


                                if (part.getType(0) == ReadableType.String) {
                                    String key = part.getString(0);

                                    if (key.equals("file")) {

                                        ReadableMap fileData = part.getMap(1);

                                        Uri _uri = Uri.parse(fileData.getString("uri"));

                                        String type = fileData.getString("type");

                                        String fileName = fileData.getString("fileName");
                                        File file = null;
                                        try {
                                            file = getTempFile(context, _uri);
                                            multipartBodyBuilder.addFormDataPart(key, fileName, RequestBody.create(MediaType.parse(type), file));

                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                    } else {
                                        String value = part.getString(1);
                                        multipartBodyBuilder.addFormDataPart(key, value);
                                    }

                                }


                            }
                            body = multipartBodyBuilder.build();
                        }
                    }

                    break;
            }

        }
        return requestBuilder
                .url(hostname)
                .method(method, body)
                .build();
    }

    public static File getTempFile(Context context, Uri uri) throws IOException {
        File file = File.createTempFile("media", null);
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1)
            outputStream.write(buffer, 0, len);
        inputStream.close();
        outputStream.close();
        return file;
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
