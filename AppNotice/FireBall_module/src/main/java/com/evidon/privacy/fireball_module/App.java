package com.evidon.privacy.fireball_module;

import android.app.Application;
import android.content.Context;

/**
 * Created by Steven.Overson on 2/24/2016.
 */
public class App extends Application {
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }

    public static Context getContext()
    {
        return context;
    }
}
