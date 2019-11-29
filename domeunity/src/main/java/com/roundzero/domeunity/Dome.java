package com.roundzero.domeunity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Size;

import com.android.volley.NetworkError;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class Dome {

    public static void MainActivityPaused() {
        //Send events to backend
        Log.d("EVENT_PAUSED", eventList.toString());
        Log.d("EVENT_PAUSED", "validation response => "+new Gson().toJson(validationResponse));
        int size = eventList.size();
        Log.d("EVENTSIZE", size+"");
        //if(size > 0) {
            eventList.get(size - 1).float_value = System.currentTimeMillis() - eventList.get(size - 1).generated_at;
        //} else {
        //    return;
        //}
        //this sets the session gap from prev session... and sets its generated_at as prev session date
        final SharedPreferences.Editor prefEditor = myPref.edit();
        long prevSessionDate = myPref.getLong(Constants.RZ_PREV_SESSION_DATE, -1);
        float prevSessionDuration = myPref.getFloat(Constants.RZ_PREV_SESSION_DURATION, -1);
        Log.d("EVENT_PREV_DATE", prevSessionDate+"");
        Log.d("EVENT_PREV_DURATION", prevSessionDuration+"");
        if (prevSessionDate > -1 && prevSessionDuration > -1) {
            long currDateFloat = eventList.get(size-1).generated_at, prevDateWithSessionFloat = prevSessionDate + (long)prevSessionDuration;
            Log.d("EVENT_CURR_DATE", currDateFloat+"");
            Log.d("EVENT_PREV_DATE_SESS", prevDateWithSessionFloat+"");
            long gap = currDateFloat - prevDateWithSessionFloat;
            eventList.get(size-1).gap_from_prev_session = gap + "";
            Log.d("EVENT_GAP", eventList.get(size-1).gap_from_prev_session);
        }
        prefEditor.putLong(Constants.RZ_PREV_SESSION_DATE, eventList.get(size-1).generated_at);
        prefEditor.putFloat(Constants.RZ_PREV_SESSION_DURATION, eventList.get(size-1).float_value);
        prefEditor.commit();

        if(Dome.validationResponse != null) {
            if (validationResponse.statusCode == 200) {
                eventsCallCounter = size;
                for (Event event : eventList) {
                    event.Submit(ctx, validationResponse.response.URI, new GetEventCallback() {
                        @Override
                        public void call(Event event) {
                            Log.d("EVENT_SUBMITTED_ONPAUSE", new Gson().toJson(event));
                            eventList.remove(event);
                            eventsCallCounter = eventsCallCounter - 1;
                            Log.d("EVENT_SUBMIT_COUNTER", eventsCallCounter+"");
                            if (eventsCallCounter == 0) {
                                Log.d("EVENT_PREFSAVE", eventList.size()+"");
                                saveEventListToPref();
                            }
                        }
                    });
                }

                if (user.submitted == false) {
                    user.Submit(ctx, validationResponse.response.URI, new GetUserCallback() {
                        @Override
                        public void call(User user) {
                            String json = new Gson().toJson(user);
                            Log.d("USER_SUBMITTED_ONPAUSE", json);
                            prefEditor.putString(Constants.RZ_NEW_USER_KEY, json);
                            prefEditor.commit();
                        }
                    });
                }
            } else {
                saveEventListToPref();
            }
        } else {
            saveEventListToPref();
        }
    }

    public static void MainActivityResumed() {
        Log.d("EVENT_RESUMED", eventList.toString());
        if (myPref == null && ctx != null) {
            myPref = ctx.getSharedPreferences(Constants.RZ_PREF, ctx.MODE_PRIVATE);
        }
        if(validationResponse != null) {
            Log.d("EVENT_RESUMED", "validation response is not null");
            if(Dome.validationResponse.statusCode == 200 && Dome.validationResponse.response.expiresAT > System.currentTimeMillis()) {
                eventsCallCounter = eventList.size();
                Log.d("EVENT_RESUMED", "total events found => "+eventsCallCounter);
                if (eventsCallCounter == 0) {
                    Validate(gameID, APIKey, env);
                }
                else for(Event event : eventList) {
                    event.Submit(ctx, Dome.validationResponse.response.URI, new GetEventCallback() {
                        @Override
                        public void call(Event event) {
                            Log.d("EVENT_SUBMITTED_ONPAUSE", new Gson().toJson(event));
                            eventsCallCounter = eventsCallCounter - 1;
                            Log.d("EVENT_PREFSAVE", eventList.size()+"");
                            eventList.remove(event);
                            if (eventsCallCounter == 0) {

                                saveEventListToPref();

                                Validate(gameID, APIKey, env/*, new GetUserCallback() {
                                    @Override
                                    public void call(User user) {
                                        Log.d("ONRESUME", new Gson().toJson(user));
                                    }
                                }*/);
                            }
                        }
                    });
                }
            } else {
                Log.d("EVENT_RESUMED", "validation response statusCode not 200 => "+new Gson().toJson(validationResponse));
            }
        } else if( gameID != null && APIKey != null ) {
            Log.d("EVENT_RESUMED", "validate start with:"+gameID+" and "+APIKey);
            Validate(gameID, APIKey, env /*, new GetUserCallback() {
                @Override
                public void call(User user) {
                    Log.d("ONRESUME", new Gson().toJson(user));
                }
            }*/);
        } else {
            Log.d("FIRST_ONRESUME", "APP launched");
        }
    }

    private static final String url = "https://devapi.round-zero.com/gtoken";
    public static String gameID;
    public static String APIKey;
    public static ValidationResponse validationResponse;
    public static List<Event> eventList;
    public static User user;
    private static int eventsCallCounter;
    public static SharedPreferences myPref;
    public static Activity ctx;
    public static String env;
    public static String CurrentState;

    public static final String version = "v1";

    public static void Validate(String game_id, String api_key, String environment /*, final GetUserCallback callback*/) {
        Log.d("SDK", "Validate method called");
        if (myPref == null) {
            myPref = ctx.getSharedPreferences(Constants.RZ_PREF, ctx.MODE_PRIVATE);
        }

        ctx.getApplication().registerActivityLifecycleCallbacks(new DomeActivityLifeCycleHandler());

        if(environment != "" && environment != Constants.RZ_ENV_DEV) {
            Log.d("ENV_ERROR", "Please use 'dev' for development and empty string '' for production.");
        }
        gameID = game_id;
        APIKey = api_key;
        env = environment;
        Referrer.GetUser(ctx , new GetUserCallback() {
            @Override
            public void call(final User u) {
                user = u;
                String json = new Gson().toJson(user);
                Log.d("GET_USER", json);
                //start event tracking...get old events and try to send them
                initNewEvent();
                volleyValidationCall(/* callback */);
            }
        });
    }

    private static void volleyValidationCall(/*final GetUserCallback callback*/) {

        RequestQueue queue = Volley.newRequestQueue(ctx);
        Log.d("INIT", "Dome validate start");

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url + "?version=" + version + "&gameID=" + gameID + "&APIkey=" + APIKey, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("RESPONSE", response);

                validationResponse = new Gson().fromJson(response, ValidationResponse.class);
                Gson gson = new Gson();
                String json = gson.toJson(validationResponse);
                SharedPreferences.Editor prefEditor = myPref.edit();
                prefEditor.putString(Constants.RZ_VALIDATION_RESPONSE, json);
                prefEditor.commit();
                //create user if not exist in backend
                //callback.call(user);

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse response = error.networkResponse;
                validationResponse = new ValidationResponse();
                if (error instanceof TimeoutError || error instanceof NoConnectionError || error instanceof NetworkError) {
                    validationResponse.statusCode = 0;
                    Log.e("VALIDATE_ERROR", "error: " + error.toString());
                } else {
                    validationResponse.statusCode = response.statusCode;
                    try {
                        String responseBody = new String(response.data, "utf-8");
                        Log.e("VALIDATE_ERROR", "Status: " + response.statusCode + " | response: " + responseBody);

                    } catch (UnsupportedEncodingException e) {
                        Log.e("VALIDATE_ERR_EXCEPTION", e.toString());
                    }
                }
                Gson gson = new Gson();
                String json = gson.toJson(validationResponse);
                SharedPreferences.Editor prefEditor = myPref.edit();
                prefEditor.putString(Constants.RZ_VALIDATION_RESPONSE, json);
                prefEditor.commit();
            }
        });

        queue.add(stringRequest);
    }

    private static void initNewEvent() {
        Event newEvent = new Event(myPref, Constants.RZ_EVENT_NEW_SESSION, user);
        newEvent.SaveEventToPref(myPref);
    }

    private static void saveEventListToPref() {
        Gson gson = new Gson();
        String json = gson.toJson(eventList);
        SharedPreferences.Editor prefEditor = myPref.edit();
        prefEditor.putString(Constants.RZ_EVENT_KEY, json);
        prefEditor.commit();
        Log.d("EVENT_LIST_SAVED", json);
    }
}
