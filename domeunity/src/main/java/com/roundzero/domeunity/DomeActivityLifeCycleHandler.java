package com.roundzero.domeunity;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

public class DomeActivityLifeCycleHandler implements Application.ActivityLifecycleCallbacks {
    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        Log.d("ON_ACTIVITY", "created "+activity.getLocalClassName());
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Log.d("ON_ACTIVITY", "started "+activity.getLocalClassName());
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.d("ON_ACTIVITY", "resumed - current dome state is "+Dome.CurrentState);
        if(Dome.CurrentState != null) {
            if(Dome.CurrentState == Constants.RZ_ACTIVITY_PAUSED) {
                Dome.CurrentState = Constants.RZ_ACTIVITY_RESUMED;
                Dome.MainActivityResumed();
            }
        } else Dome.CurrentState = Constants.RZ_ACTIVITY_RESUMED;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.d("ON_ACTIVITY", "paused - current dome state is "+Dome.CurrentState);
        if(Dome.CurrentState != null) {
            if(Dome.CurrentState == Constants.RZ_ACTIVITY_RESUMED) {
                Dome.CurrentState = Constants.RZ_ACTIVITY_PAUSED;
                Dome.MainActivityPaused();
            }
        } else {
            Dome.CurrentState = Constants.RZ_ACTIVITY_PAUSED;
            Dome.MainActivityPaused();
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Log.d("ON_ACTIVITY", "stopped "+activity.getLocalClassName());
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {
        Log.d("ON_ACTIVITY", "save instance state "+activity.getLocalClassName());
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.d("ON_ACTIVITY", "destroyed "+activity.getLocalClassName());
    }
}

