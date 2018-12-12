package com.toyberman.Utils;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;


import com.toyberman.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.Request;

/**
 * Created by Max Toyberman on 2/5/18.
 */

public class Utilities {

    // Copy an InputStream to a File.
    public static void copyInputStreamToFile(InputStream in, File file) {
        OutputStream out = null;

        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        } finally {
            // Ensure that the InputStreams are closed even if there's an exception.
            try {
                if (out != null) {
                    out.close();
                }

                // If you want to close the "in" InputStream yourself then remove this
                // from here but ensure that you close it yourself eventually.
                in.close();
            } catch (IOException e) {
                if (BuildConfig.DEBUG) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * @param map     - map of headers
     * @param builder - request builder, all headers will be added to this request
     */
    public static void addHeadersFromMap(ReadableMap map, Request.Builder builder) {

        ReadableMapKeySetIterator iterator = map.keySetIterator();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            builder.addHeader(key, map.getString(key));
        }
    }

}
