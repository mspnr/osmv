package com.applikationsprogramvara.osmviewer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Utils {

    public static void openMarketLink(Context context) {
        String str1 = "market://details?id=" + context.getPackageName();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(str1));
        context.startActivity(browserIntent);
    }


}
