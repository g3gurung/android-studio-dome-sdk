package com.roundzero.domeunity;


import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Event {
    public UUID insertId;
    public UUID user_id;
    public String game_id;
    public String event_name;
    public String device_os;
    public String device_os_version;
    public String country;
    public long generated_at;

    public String referrer_string;
    public float float_value;
    public String string_value;
    public int session_count;
    public int retention_day_count;
    public long first_session_date;
    public String gap_from_prev_session = ""; //String because empty string wont parse to number(but will become null) for first event in the backend


    public String campaign_name;
    public String campaign_source;
    public String campaign_medium;
    public String promo_net;
    public String country_set;

    public String env;

    public Event(SharedPreferences myPref, String event_name, User user) {
        this.env = Dome.env;

        this.insertId = UUID.randomUUID();
        this.user_id = user.insertId;
        this.game_id = Dome.gameID;
        this.event_name = event_name;
        this.device_os = user.device_os;
        this.device_os_version = user.device_os_version;
        this.country = user.country;
        this.generated_at = System.currentTimeMillis();

        //get data from user obj
        this.referrer_string = user.referrer_string;
        this.campaign_name = user.campaign_name;
        this.campaign_source = user.campaign_source;
        this.campaign_medium = user.campaign_medium;
        this.promo_net = user.promo_net;
        this.country_set = user.country_set;

        int sessionCounter = myPref.getInt(Constants.RZ_EVENT_SESSION_COUNTER, 1);
        this.session_count = sessionCounter;
        sessionCounter = 1 + sessionCounter;

        SharedPreferences.Editor prefEditor = myPref.edit();
        prefEditor.putInt(Constants.RZ_EVENT_SESSION_COUNTER, sessionCounter);


        long firstSessionDate = myPref.getLong(Constants.RZ_FIRST_SESSION_DATE, this.generated_at);
        this.first_session_date = firstSessionDate;

        /*
        long prevSessionDate = myPref.getLong(Constants.RZ_PREV_SESSION_DATE, -1), prevSessionDuration = myPref.getLong(Constants.RZ_PREV_SESSION_DURATION, -1);
        if (prevSessionDate > -1 && prevSessionDuration > -1) {
            this.gap_from_prev_session = (this.generated_at - prevSessionDate - prevSessionDuration) + "";
        }
        prefEditor.putLong(Constants.RZ_PREV_SESSION_DATE, this.generated_at);
        */

        int retentionDayCounter = myPref.getInt(Constants.RZ_RET_DAY_COUNTER, 0);
        this.retention_day_count = retentionDayCounter;
        retentionDayCounter = (int) ((this.generated_at - this.first_session_date)/(24*60*60*1000)); //this counts the actual retention day of the event
        prefEditor.putInt(Constants.RZ_RET_DAY_COUNTER, retentionDayCounter);

        prefEditor.commit();

    }

    public static List<Event> GetEvents(SharedPreferences myPref) {
        List<Event> eventList = new ArrayList<Event>();
        SharedPreferences.Editor prefEditor = myPref.edit();
        String json = myPref.getString(Constants.RZ_EVENT_KEY, "");
        if (json != "") {
            Gson gson = new Gson();
            eventList = gson.fromJson(json, new TypeToken<ArrayList<Event>>() {
            }.getType());
        }
        return eventList;
    }

    public void SaveEventToPref(SharedPreferences myPref) {
        Dome.eventList = GetEvents(myPref);
        Dome.eventList.add(this);
        Gson gson = new Gson();
        String json = gson.toJson(Dome.eventList);
        SharedPreferences.Editor prefEditor = myPref.edit();
        prefEditor.putString(Constants.RZ_EVENT_KEY, json);
        prefEditor.commit();
        Log.d("EVENT_SAVED_TO_PREF", new Gson().toJson(Dome.eventList));
    }

    public static List<Event> RemoveEvents(SharedPreferences myPref, List<UUID> ids) {
        List<Event> eventList = GetEvents(myPref);
        for (UUID id : ids) {
            for(int counter = 0; counter < eventList.size(); counter ++) {
                Event event = eventList.get(counter);
                if (event.insertId == id) {
                    eventList.remove(counter);
                    break;
                }
            }
        }
        Gson gson = new Gson();
        String json = gson.toJson(eventList);
        SharedPreferences.Editor prefEditor = myPref.edit();
        prefEditor.putString(Constants.RZ_EVENT_KEY, json);
        prefEditor.commit();
        return eventList;
    }

    private PubPayload getPayload() {
        Gson gson = new Gson();
        String jsonStr = gson.toJson(this);

        PubData pubData = new PubData(Base64.encodeToString(jsonStr.getBytes(), Base64.NO_WRAP));

        PubPayload pubPayload = new PubPayload(pubData);

        return pubPayload;
    }

    public void Submit(Context ctx, String uri, final GetEventCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(ctx);
        Gson gson = new Gson();
        String json = gson.toJson(this.getPayload());
        try {
            final Event self = this;
            Log.d("EVENT_SUBMITTING", json);
            final JSONObject jsonObject = new JSONObject();
            jsonObject.put("username", "sdk-dome-test");
            //JSONObject jsonObject = new JSONObject(json);
            jsonObject.put("text", "`" + new Gson().toJson(self) + "`");
            uri = "https://hooks.slack.com/services/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";
            /*
            JsonObjectRequest jsonReq = new JsonObjectRequest((Request.Method.POST), uri, jsonObject, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d("EVENT_SUBMIT_SUCCESS", response.toString());
                    Dome.eventList.remove(self);

                    callback.call(self);
                }}, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("EVENT_SUBMIT_ERROR", error.toString());
                    try {
                        NetworkResponse response = error.networkResponse;
                        String responseBody = new String(response.data, "utf-8");
                        Log.e("EVENT_RESPONSE_ERROR", "Status: " + response.statusCode + " | response: " + responseBody);

                    } catch (UnsupportedEncodingException e) {
                        Log.e("EVENT_ERR_EXCEPTION", e.toString());
                    }
                    callback.call(self);
                }
            });
            queue.add(jsonReq);
            */
            StringRequest stringRequest = new StringRequest(Request.Method.POST, uri,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d("EVENT_SUBMIT_SUCCESS", response.toString());
                            //Dome.eventList.remove(self);

                            callback.call(self);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {

                }
            }){
                @Override
                public byte[] getBody() throws AuthFailureError {
                    return jsonObject.toString().getBytes();
                }
            };
            queue.add(stringRequest);
        } catch (org.json.JSONException e) {
            Log.e("SAVE_EVENT_TO_JSON_ERR", e.toString());
        }
    }
}
