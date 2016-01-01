package com.github.ashkansarlak.recmodplay;

import android.app.Application;

/**
 * Created by Ashkan on 12/29/2015.
 */
public class App extends Application {
    private static App staticRef;

    @Override
    public void onCreate() {
        super.onCreate();
        App.staticRef = this;
    }

    public static App get() {
        return staticRef;
    }
}
