package com.bnctech.blelocker;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

public class HttpsProtocol {
//    private static final String appServerUrl           = "https://unicornbike1001.com:49999/console";
//    public static final String notiServerBackgroundUrl = "https://unicornbike1001.com:10101/test_bg/";
//    public static final String notiServerImageUrl      = "https://unicornbike1001.com:10101/test_img/";
//    public static final String parkingPictureUrl       = "https://unicornbike1001.com:10200/app/parking_picture";

    private static final String appServerUrl           = "https://bnc-iot.com:49999/console";
    public static final String notiServerBackgroundUrl = "https://bnc-iot.com:10101/test_bg/";
    public static final String notiServerImageUrl      = "https://bnc-iot.com:10101/test_img/";
    public static final String parkingPictureUrl       = "https://bnc-iot.com:10200/app/parking_picture";

    private static final String appServerMethod = "POST";

    private static final String appServerHeader_ContentTypeKey   = "content-type";
    private static final String appServerHeader_ContentTypeValue = "application/json";
    private static final String appServerHeader_TimestampKey     = "bx-timestamp-v1";
    private static final String appServerHeader_SignatureKey     = "bx-signature-v1";

    public static final String appServerBody_RequestMessageKey     = "req_msg";
    public static final String appServerBody_AppVersion            = "console_app_version";
    public static final String appServerBody_TagKey                = "tag";
    public static final String appServerBody_DeviceIdKey           = "device_id";
    public static final String appServerBody_PhoneNumberKey        = "phone_num";
    public static final String appServerBody_ManagerPhoneNumberKey = "phone_number";
    public static final String appServerBody_UserIdKey             = "user_id";
    public static final String appServerBody_AdminPhoneKey         = "admin_phone";
    public static final String appServerBody_QRcodeKey             = "qr_code";
    public static final String appServerBody_OperatingStateKey     = "operating_state";
    public static final String appServerBody_OneDeviceID           = "device_id";
    public static final String appServerBody_OneDeviceMessage      = "message";
    public static final String appServerBody_OneDeviceFromDate     = "fromdate";
    public static final String appServerBody_OneDeviceToDate       = "todate";
    public static final String appServerBody_PaymentStartTime      = "start_time";
    public static final String appServerBody_PaymentEndTime        = "end_time";
    public static final String appServerBody_SetStateKey           = "set_state";
    public static final String appServerBody_Latitude              = "latitude";
    public static final String appServerBody_Longitude             = "longitude";
    public static final String appServerBody_TagValue              = "DALONG";

    public static final int appServerBody_RequestMessageValue_AdminConfirm             = 3;
    public static final int appServerBody_RequestMessageValue_AllDevice                = 11;
    public static final int appServerBody_RequestMessageValue_OneDevice_rcvData        = 12;
    public static final int appServerBody_RequestMessageValue_GetQRcode                = 14;
    public static final int appServerBody_RequestMessageValue_SetUserState             = 30;
    public static final int appServerBody_RequestMessageValue_GetPayment               = 34;
    public static final int appServerBody_RequestMessageValue_BikeOperatingStateChange = 51;
    public static final int appServerBody_RequestMessageValue_RegistParkingPicture     = 52;
    public static final int appServerBody_RequestMessageValue_GetNotification          = 129;
    public static final int appServerBody_RequestMessageValue_GetLastCellFinder        = 141;
    public static final int appServerBody_RequestMessageValue_GetLastHistory           = 142;

    public static final int RES_CODE__SUCCESS          = 200;
    public static final int RES_CODE__ERROR            = 400;
    public static final int RES_CODE__UPDATE           = 401;
    public static final int RES_CODE__NOT_MATCHED_IDPW = 402;

    private static final int APP_SERVER_CONNECTION_TIMEOUT = 10000;

    public static Object[] https_request(JSONObject body) {
        Object[] rtnValue = new Object[2];

        try {
            URL url = new URL(appServerUrl);
            HttpsURLConnection urlConn = (HttpsURLConnection) url.openConnection();
            String nowDate = Long.toString(System.currentTimeMillis());

            urlConn.setRequestMethod(appServerMethod);
            urlConn.setRequestProperty(appServerHeader_ContentTypeKey, appServerHeader_ContentTypeValue);
            urlConn.setRequestProperty(appServerHeader_TimestampKey, nowDate);
            urlConn.setRequestProperty(appServerHeader_SignatureKey, Encryption.encrypt_header(nowDate));

            body.put(appServerBody_ManagerPhoneNumberKey, MainActivity.userData_PhoneNumber);

            OutputStream os = urlConn.getOutputStream();
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            urlConn.setConnectTimeout(APP_SERVER_CONNECTION_TIMEOUT);
            int resCode = urlConn.getResponseCode();
            rtnValue[0] = resCode;
            BufferedReader clientReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), StandardCharsets.UTF_8));
            String resMsg = clientReader.readLine();

            rtnValue[1] = resMsg;
        } catch (Exception e) {
            Log.e("https_request", e.toString());
        }

        return rtnValue;
    }

    public static int picture_request(File file, String device_id, String user_id) {
        int resCode = RES_CODE__ERROR;

        try {
            URL url = new URL(parkingPictureUrl);
            HttpsURLConnection urlConn = (HttpsURLConnection) url.openConnection();
            String nowDate = Long.toString(System.currentTimeMillis());
            String[] meta = {
                    ("--" + nowDate + "\r\n"),
                    ("Content-Disposition: form-data; name=\"file\"; filename=\"" + device_id + "-" + user_id + "-manager.jpg" + "\"\r\n"),
                    ("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName()) + "\r\n"),
                    ("Content-Transfer-Encoding: binary\r\n\r\n"),
                    ("\r\n"),
                    ("--" + nowDate + "--\r\n")
            };

            urlConn.setDoOutput(true);
            urlConn.setDoInput(true);
            urlConn.setUseCaches(false);

            urlConn.setRequestMethod(appServerMethod);
            urlConn.setRequestProperty(appServerHeader_TimestampKey, nowDate);
            urlConn.setRequestProperty(appServerHeader_SignatureKey, Encryption.encrypt_header(nowDate));
            urlConn.setRequestProperty("ENCTYPE", "multipart/form-data");
            urlConn.setRequestProperty(appServerHeader_ContentTypeKey, "multipart/form-data; charset=utf-8; boundary=" + nowDate);

            OutputStream os = urlConn.getOutputStream();
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8), true);

            pw.append(meta[0]);
            pw.append(meta[1]);
            pw.append(meta[2]);
            pw.append(meta[3]);
            pw.flush();

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            int length = -1;
            while ((length = fis.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
            os.flush();
            fis.close();
            pw.append(meta[4]);
            pw.flush();

            pw.append(meta[5]);
            pw.close();

            resCode = urlConn.getResponseCode();
        } catch (Exception e) {
            Log.e("picture_request", e.toString());
        }

        return resCode;
    }
}
