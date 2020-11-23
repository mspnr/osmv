package com.applikationsprogramvara.osmviewer;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;

import org.osmdroid.util.GeoPoint;

public class Utils {

    public static void openMarketLink(Context context) {
        String str1 = "market://details?id=" + context.getPackageName();
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(str1));
        context.startActivity(browserIntent);
    }


    static String distanceToStr(double distance, Context context) {

        switch (EnhancedSharedPreferences.getDefaultSharedPreferences(context).getString("UnitsOfMeasure", "metric")) {
            default:
            case "metric":
                if (distance >= 10000)
                    return "" + keepTwoDigits(distance) / 1000 + " " + context.getString(R.string.unit_km);
                else if (distance >= 1000)
                    return "" + (float) keepTwoDigits(distance) / 1000 + " " + context.getString(R.string.unit_km);
                else return "" + keepTwoDigits(distance) + " " + context.getString(R.string.unit_meter);
            case "imperial":
                double MILE = 1609.344;
                double YARD = 0.9144;
                if (distance >= 10 * MILE)
                    return "" + keepTwoDigits(distance / MILE) + " " + context.getString(R.string.unit_mile);
                else if (distance >= MILE)
                    return "" + (float) keepTwoDigits(10 * distance / MILE ) / 10 + " " + context.getString(R.string.unit_mile);
                else return "" + keepTwoDigits(distance / YARD) + " " + context.getString(R.string.unit_yard);
        }
    }

    private static int keepTwoDigits(double distance) {
        int digits = 0;
        while (distance >= 100) {
            distance = distance / 10;
            digits++;
        }

        int result = (int) (Math.floor(distance) * Math.pow(10, digits));

        return result;
    }

    static float distance(GeoPoint pa, GeoPoint pb) {
        Location a = new Location("A");
        a.setLatitude(pa.getLatitude());
        a.setLongitude(pa.getLongitude());

        Location b = new Location("B");
        b.setLatitude(pb.getLatitude());
        b.setLongitude(pb.getLongitude());

        return a.distanceTo(b);
    }
}
