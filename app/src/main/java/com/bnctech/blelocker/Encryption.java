package com.bnctech.blelocker;

import android.util.Base64;
import android.util.Log;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Encryption {
    private static final String algorithm = "HmacSHA256";
    private static final String access_key = "EVTfAkO1b8Ne7qIz5Y6c";
    private static final String secret_key = "qDUB08P6S1vdjEFvN9lvxJaV4Vj5mHcxORxV9kQd";

    public static String encrypt_header(String date) {
        try {
            Mac hmac = Mac.getInstance(algorithm);
            hmac.init(new SecretKeySpec(secret_key.getBytes(), algorithm));

            String body = "POST BNCTECH\n" + date + "\n" + access_key;

            return Base64.encodeToString(hmac.doFinal(body.getBytes()), Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e("encrypt_header", e.toString());
        }

        return "";
    }
}