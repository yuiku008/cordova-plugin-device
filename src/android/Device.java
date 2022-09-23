/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package org.apache.cordova.device;

import java.util.TimeZone;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.cordova.LOG;

import android.provider.Settings;
import android.os.Build;
import android.text.TextUtils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

public class Device extends CordovaPlugin {
    public static final String TAG = "Device";

    public static String platform; // Device OS
    public static String uuid; // Device UUID

    private static final String ANDROID_PLATFORM = "Android";
    private static final String AMAZON_PLATFORM = "amazon-fireos";
    private static final String AMAZON_DEVICE = "Amazon";
    private static final String LOG_TAG = "DEVICE_INFO";
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";

    /**
     * Constructor.
     */
    public Device() {
    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action          The action to execute.
     * @param args            JSONArry of arguments for the plugin.
     * @param callbackContext The callback id used when calling back into
     *                        JavaScript.
     * @return True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if ("getDeviceInfo".equals(action)) {
            String uuid = getUuid();
            Device.uuid = uuid;
            JSONObject r = new JSONObject();
            r.put("uuid", uuid);
            r.put("version", this.getOSVersion());
            r.put("platform", this.getPlatform());
            r.put("fingerprint ", Build.FINGERPRINT);
            r.put("model", this.getModel());
            r.put("manufacturer", this.getManufacturer());
            r.put("brand ", Build.BRAND);
            r.put("device ", Build.DEVICE);
            r.put("isVirtual", this.isVirtual() || this.getCpuInfo());
            r.put("serial", this.getSerialNumber());
            callbackContext.success(r);
            LOG.d(LOG_TAG, r.toString());
        } else {
            return false;
        }
        return true;
    }

    // --------------------------------------------------------------------------
    // LOCAL METHODS
    // --------------------------------------------------------------------------

    /**
     * Get the OS name.
     *
     * @return
     */
    public String getPlatform() {
        String platform;
        if (isAmazonDevice()) {
            platform = AMAZON_PLATFORM;
        } else {
            platform = ANDROID_PLATFORM;
        }
        return platform;
    }

    /**
     * Get the device's Universally Unique Identifier (UUID).
     *
     * @return
     */
    public String getUuid() {
        // String uuid =
        // Settings.Secure.getString(this.cordova.getActivity().getContentResolver(),
        // android.provider.Settings.Secure.ANDROID_ID);
        return this.getDeviceId();
    }

    protected String getDeviceId() {
        String uuid = null;
        try {
            Context context = this.cordova.getActivity().getApplicationContext();
            SharedPreferences sharedPrefs = context.getSharedPreferences(PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uuid = sharedPrefs.getString(PREF_UNIQUE_ID, null);

            if (uuid == null) {
                uuid = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uuid);
                editor.commit();
            }
            return uuid; 
        } catch (Exception e) {
            uuid =  UUID.randomUUID().toString();
        } finally {
           return uuid;
        }
    }

    public String getModel() {
        String model = Build.MODEL;
        return model;
    }

    public String getProductName() {
        String productname = Build.PRODUCT;
        return productname;
    }

    public String getManufacturer() {
        String manufacturer = Build.MANUFACTURER;
        return manufacturer;
    }

    public String getSerialNumber() {
        String serial = Build.SERIAL;
        return serial;
    }

    /**
     * Get the OS version.
     *
     * @return
     */
    public String getOSVersion() {
        String osversion = Build.VERSION.RELEASE;
        return osversion;
    }

    public String getSDKVersion() {
        @SuppressWarnings("deprecation")
        String sdkversion = Build.VERSION.SDK;
        return sdkversion;
    }

    public String getTimeZoneID() {
        TimeZone tz = TimeZone.getDefault();
        return (tz.getID());
    }

    /**
     * Function to check if the device is manufactured by Amazon
     *
     * @return
     */
    public boolean isAmazonDevice() {
        if (Build.MANUFACTURER.equals(AMAZON_DEVICE)) {
            return true;
        }
        return false;
    }

    public boolean isVirtual() {
        // 验证build信息内 true 模拟器 false 甄姬
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("sdk_gphone64_arm64")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("vbox")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator");
    }

    /**
     * 根据CPU是否为电脑来判断是否为模拟器(子方法)
     * 返回:false 真机
     * true 模拟器
     */
    public static boolean getCpuInfo() {
        String result = "";
        boolean flag = false;
        try {
            String[] args = { "/system/bin/cat", "/proc/cpuinfo" };
            ProcessBuilder cmd = new ProcessBuilder(args);
            Process process = cmd.start();
            StringBuffer sb = new StringBuffer();
            String readLine = "";
            BufferedReader responseReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "utf-8"));
            while ((readLine = responseReader.readLine()) != null) {
                sb.append(readLine);
                if (!TextUtils.isEmpty(readLine)) {
                    if (readLine.contains("Hardware")) {
                        if (readLine.contains("placeholder")) {
                            flag = true;
                        }
                    } else if (readLine.contains("Revision")) {
                        if (readLine.contains("000b")) {
                            flag = true;
                        }
                    }
                }
            }
            responseReader.close();
            result = sb.toString().toLowerCase();
        } catch (IOException ex) {

        }
        if (flag) {
            LOG.d(LOG_TAG, "getCpuInfo TRUE");
        }

        return flag;
    }

}