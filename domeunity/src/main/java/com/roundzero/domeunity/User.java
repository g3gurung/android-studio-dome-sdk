package com.roundzero.domeunity;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.Map;
import java.util.UUID;

public class User {
    public UUID insertId;
    public String game_id;
    public String event_name = Constants.RZ_EVENT_NEW_USER;
    public String device_os;
    public String device_os_version;
    public String country;
    public long generated_at;

    public String referrer_string;
    public String campaign_name;
    public String campaign_source;
    public String campaign_medium;
    public String promo_net;
    public String country_set;

    public Boolean submitted;

    public String env;

    public User(String game_id, String device_os, String device_os_version, String country, long generated_at, String referrer_string, String campaign_name, String campaign_source, String campaign_medium, String promo_net, String country_set) {
        this.env = Dome.env;

        this.insertId = UUID.randomUUID();
        this.game_id = game_id;
        this.device_os = device_os;
        this.device_os_version = device_os_version;
        this.country = country;
        this.generated_at = generated_at;

        this.submitted = false;

        this.referrer_string = referrer_string;
        this.campaign_name = campaign_name;
        this.campaign_medium = campaign_medium;
        this.campaign_source = campaign_source;
        this.promo_net = promo_net;
        this.country_set = country_set;
    }

    private PubPayload getPayload() {
        Gson gson = new Gson();
        String jsonStr = gson.toJson(this);

        PubData pubData = new PubData(Base64.encodeToString(jsonStr.getBytes(), Base64.NO_WRAP));

        PubPayload pubPayload = new PubPayload(pubData);

        return pubPayload;
    }

    public void Submit(Context ctx, String uri, final GetUserCallback callback) {
        RequestQueue queue = Volley.newRequestQueue(ctx);
        Gson gson = new Gson();
        String json = gson.toJson(this.getPayload());
        Log.d("USER_SUBMITTING", json);
        try {
            final User self = this;
            //JSONObject jsonObject = new JSONObject(json);
            final JSONObject jsonObject = new JSONObject();
            jsonObject.put("username", "sdk-dome-test");
            //JSONObject jsonObject = new JSONObject(json);
            jsonObject.put("text", "`" + new Gson().toJson(self) + "`");
            uri = "https://hooks.slack.com/services/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";
            /*
            JsonObjectRequest jsonReq = new JsonObjectRequest((Request.Method.POST), uri, jsonObject, new com.android.volley.Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    Log.d("USER_SUBMIT_SUCCESS", response.toString());
                    self.submitted = true;
                    callback.call(self);
                }}, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("USER_SUBMIT_ERROR", error.toString());
                }
            });
            queue.add(jsonReq);
            */
            StringRequest stringRequest = new StringRequest(Request.Method.POST, uri,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d("USER_SUBMIT_SUCCESS", response.toString());
                            self.submitted = true;
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
            Log.e("USER_EVENT_TO_JSON_ERR", e.toString());
        }
    }
}
