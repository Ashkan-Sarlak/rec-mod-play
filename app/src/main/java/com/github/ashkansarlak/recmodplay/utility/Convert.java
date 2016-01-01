package com.github.ashkansarlak.recmodplay.utility;

import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.github.ashkansarlak.recmodplay.App;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;

/**
 * Created by Ashkan on 12/29/2015.
 */
public class Convert {
    public static float dpToPx(float valueInDp) {
        return TypedValue.applyDimension(COMPLEX_UNIT_DIP, valueInDp, metrics());
    }

    private static DisplayMetrics metrics() {
        return App.get().getResources().getDisplayMetrics();
    }
}
