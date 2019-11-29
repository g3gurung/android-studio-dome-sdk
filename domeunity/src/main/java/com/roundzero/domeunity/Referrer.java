package com.roundzero.domeunity;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.google.gson.Gson;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Locale;

public class Referrer {

    public static void GetUser(final Context ctx, final GetUserCallback callback) {
        final Gson gson = new Gson();
        final SharedPreferences.Editor prefEditor = Dome.myPref.edit();
        String json = Dome.myPref.getString(Constants.RZ_NEW_USER_KEY, "");

        if(json == "") {
            Log.d("GETTING_USER", "empty");
            User user;
            final InstallReferrerClient referrerClient = InstallReferrerClient.newBuilder(ctx).build();
            referrerClient.startConnection(new InstallReferrerStateListener() {
                @Override
                public void onInstallReferrerSetupFinished(int responseCode) {
                    Locale locale;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        locale = ctx.getResources().getConfiguration().getLocales().get(0);
                    } else {
                        locale = ctx.getResources().getConfiguration().locale;
                    }
                    String country = locale.getCountry();
                    User user = new User(Dome.gameID, Constants.RZ_ANDROID, Build.VERSION.RELEASE, country,
                            System.currentTimeMillis(), "", "", "", "", "", "");
                    switch (responseCode) {
                        case InstallReferrerClient.InstallReferrerResponse.OK:
                            try {
                                ReferrerDetails response = referrerClient.getInstallReferrer();
                                String referrerString = response.getInstallReferrer();
                                HashMap<String, String> parsedReferrerString = parseQueryStrings(referrerString);

                                Log.d("REFERRER_API", referrerString);
                                long clickTS = response.getReferrerClickTimestampSeconds(), installBeginTS = response.getInstallBeginTimestampSeconds();
                                Log.d("CLICKTS", clickTS+"");
                                Log.d("INSTALL_BEGIN", installBeginTS+"");

                                String source = parsedReferrerString.get("utm_source") == null ? "":parsedReferrerString.get("utm_source");
                                String medium = parsedReferrerString.get("utm_medium") == null ? "":parsedReferrerString.get("utm_medium");
                                String campaign = parsedReferrerString.get("utm_campaign") == null ? "":parsedReferrerString.get("utm_campaign");
                                String content = parsedReferrerString.get("utm_content") == null ? "":parsedReferrerString.get("utm_content");
                                String gameID = "";
                                String promoNet = "";
                                String countrySet = "";

                                String[] contentSlice;
                                if (content != "") {
                                    contentSlice = content.split("__");
                                    if (contentSlice.length == 2){
                                        gameID = contentSlice[0].trim();
                                        promoNet = contentSlice[1].trim();
                                    } else if (contentSlice.length == 3) {
                                        gameID = contentSlice[0].trim();
                                        promoNet = contentSlice[1].trim();
                                        countrySet = contentSlice[2].trim();
                                    }
                                }

                                Log.d("UTM", source+" "+medium+" "+campaign+" "+gameID+" "+promoNet);

                                user.referrer_string = referrerString;
                                if (Dome.gameID.compareTo(gameID) == 0) {
                                    //Attribution complete
                                    user.campaign_source = source; user.campaign_name = campaign; user.campaign_medium = medium; user.promo_net = promoNet; user.country_set = countrySet;
                                }
                                prefEditor.putString(Constants.RZ_NEW_USER_KEY, gson.toJson(user));
                                prefEditor.commit();
                                callback.call(user);
                            } catch(android.os.RemoteException e) {
                                Log.d("ERROR_REFERRER_STRING", e.getMessage());
                            }
                            break;

                        case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                            //Read referrer string from old way using manifest
                            Log.d("REFERRER_API", "FEATURE_NOT_SUPPORTED");
                            prefEditor.putString(Constants.RZ_NEW_USER_KEY, gson.toJson(user));
                            prefEditor.commit();
                            user.campaign_source = "feature not supported";
                            callback.call(user);
                            break;

                        case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                            //Read referrer string from old way using manifest
                            Log.d("REFERRER_API", "SERVICE_UNAVAILABLE");
                            prefEditor.putString(Constants.RZ_NEW_USER_KEY, gson.toJson(user));
                            prefEditor.commit();
                            user.campaign_source = "service unavailable";
                            callback.call(user);
                            break;
                    }
                }

                @Override
                public void onInstallReferrerServiceDisconnected() {
                    //Redo the call
                    Referrer.GetUser(ctx, callback);
                }
            });
        } else {
            Log.d("GETTING_USER", json);
            User user = gson.fromJson(json, User.class);
            callback.call(user);
        }
    }

    private static HashMap<String, String> parseQueryStrings(String queryStrings) {
        HashMap<String, String> parsedQueryStrings = new HashMap<>();
        try {

            Uri uri = Uri.parse(URLDecoder.decode("https://round-zero.com/?" + queryStrings, "UTF-8"));
            if (uri != null) {
                for (String key : uri.getQueryParameterNames()) {
                    parsedQueryStrings.put(key, uri.getQueryParameter(key));
                    System.out.println("key=[" + key + "], value=[" + uri.getQueryParameter(key) + "]");
                }
            }
        } catch (java.io.UnsupportedEncodingException e) {
            Log.e("REFERRAR_PARSE_ERR", e.toString());
        }
        return parsedQueryStrings;
    }
}

