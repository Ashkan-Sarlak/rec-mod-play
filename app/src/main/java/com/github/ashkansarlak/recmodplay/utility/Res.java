package com.github.ashkansarlak.recmodplay.utility;

import com.github.ashkansarlak.recmodplay.App;

/**
 * Created by Ashkan on 12/29/2015.
 */
public class Res {
    public static int getColor(int colorResId) {
        return App.get().getResources().getColor(colorResId);
    }
}
