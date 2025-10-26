package com.applikationsprogramvara.osmviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import org.xmlpull.v1.XmlPullParser;

public class SettingsActivity extends AppCompatActivity {

    public static final String ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android";

    static Activity context;
    private MyPreferenceFragment preferenceFragment;
    private static SharedPreferences prefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);

        context = this;

        prefs = PreferenceManager.getDefaultSharedPreferences(context);

        preferenceFragment = new MyPreferenceFragment(this);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, preferenceFragment)
                .commit();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                saveAndClose();
                break;
        }
        return true;
    }

    @Override
    public boolean onSupportNavigateUp(){
        saveAndClose();
        return true;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        saveAndClose();
    }

    private void saveAndClose() {
        Intent resultIntent = new Intent();
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }


    public static class MyPreferenceFragment extends PreferenceFragmentCompat {


        private final SettingsActivity settingsActivity;

        public MyPreferenceFragment(SettingsActivity settingsActivity) {
            this.settingsActivity = settingsActivity;
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            init();
        }

        public void init() {
            addPreferencesFromResource(R.xml.settings);

            String[] values2bind = new String[]{
                    "UnitsOfMeasure"
            };

            for(String value: values2bind)
                bindPreferenceSummaryToValue(findPreference(value));

            if (!BuildConfig.DEBUG) {
//                PreferenceScreen mainScreen = findPreference("mainScreen");
//                mainScreen.removePreference(findPreference(""));
            } else {
//                findPreference("").setOnPreferenceClickListener(preference -> {
//                });
            }


            setMapSource(0);
            setMapSource(1);
            setOverlaySource();

            Preference marketLinkPreference = findPreference("MarketLink");
            marketLinkPreference.setOnPreferenceClickListener(preference -> {
                Utils.openMarketLink(context);
                return true;
            });

            String version;
            String libVersion;
            try {
                version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
                //implementation 'org.osmdroid:osmdroid-android:6.1.6'
                libVersion = context.getPackageManager().getPackageInfo("org.osmdroid", 0).versionName;
                Log.d("MyApp3", "droid version " + org.osmdroid.views.MapView.class.getPackage().getImplementationVersion());
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("MyApp", e.toString());
                version = "unknown";
                libVersion = "unknown";
            }

            Preference about_pref = findPreference("About");
            about_pref.setSummary(
                    "- " + settingsActivity.getString(R.string.app_name) + ": " + version + (BuildConfig.DEBUG ? " debug" : "") + "\n" +
                    "- " + settingsActivity.getString(R.string.stg_version_lib) + ": " + libVersion
            );

            findPreference("Hint1LongTap").setOnPreferenceClickListener(preference -> {
                Toast.makeText(getContext(), "No additional information here", Toast.LENGTH_LONG).show();
                return false;
            });

            findPreference("Hint2Caching").setOnPreferenceClickListener(preference -> {
                new AlertDialog.Builder(getContext())
                        .setTitle("Question")
                        .setMessage("You can get some additional information on OSMDroid support page. Would you like to open the web-page in your browser?")
                        .setPositiveButton("Open", (dialog, which) -> {
                            String address = "https://osmdroid.github.io/osmdroid/Tile-Caching.html";
                            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(address));
                            getContext().startActivity(browserIntent);
                        })
                        .setNegativeButton("Back", null)
                        .show();
                return false;
            });

        }

        private void setMapSource(int index1) {
            Preference map1 = findPreference("MapSource" + index1);
            MapsCatalog mapsCatalog = new MapsCatalog(map1.getSharedPreferences(), (source, image, index) -> map1.setSummary(source.name()), null);
            mapsCatalog.load(index1);
            map1.setOnPreferenceClickListener(preference -> {
                mapsCatalog.selectSource(context, index1);
                return true;
            });
        }

        private void setOverlaySource() {
            Preference map1 = findPreference("OverlaySource");
            MapsCatalog mapsCatalog = new MapsCatalog(map1.getSharedPreferences(), null, (source, show) -> map1.setSummary(source.name()));
            mapsCatalog.loadOverlay();
            map1.setOnPreferenceClickListener(preference -> {
                mapsCatalog.selectOverlay(context);
                return true;
            });
        }

    }

    private static void bindPreferenceSummaryToValue(Preference preference) {
        if (preference == null)
            return;

        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getString(preference.getKey(), ""));
    }


    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (preference, value) -> {
        String stringValue = value.toString();

        if (preference instanceof ListPreference) {
            // For list preferences, look up the correct display value in
            // the preference's 'entries' list.
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);

            // Set the summary to reflect the new value.
            preference.setSummary(
                    index >= 0
                            ? listPreference.getEntries()[index]
                            : null);

        } else if (preference instanceof EditTextPreference) {
            EditTextPreference editTextPreference = (EditTextPreference) preference;

            boolean floatPreference = //false;
                    //(editTextPreference.getEditText().getInputType() & InputType.TYPE_NUMBER_FLAG_DECIMAL) > 0;
                    //android:inputType="numberDecimal"
                    getETAttribute(editTextPreference.getKey(), "inputType").equals("0x2002");
            // inputType - numberDecimal - 0x2002

            boolean integerPreference = //false;
                    //(editTextPreference.getEditText().getInputType() & InputType.TYPE_CLASS_NUMBER) > 0;
                    // android:numeric="integer"
                    getETAttribute(editTextPreference.getKey(), "numeric").equals("0x1");
            // numeric - integer - 0x1

            if (floatPreference || integerPreference) {

                float[] extremums = getExtremums(preference.getKey());
                float min = extremums[0];
                float max = extremums[1];
                if ((min != 0) && (max != 0) && (min < max)) {
                    boolean correction = false;

                    float newFloatValue = StrToFloat((String) value, 0);
                    if (newFloatValue < min) {
                        value = min;
                        correction = true;
                        Toast.makeText(context, R.string.toast_error_value_low, Toast.LENGTH_LONG).show();
                    } else if (max < newFloatValue) {
                        value = max;
                        correction = true;
                        Toast.makeText(context, R.string.toast_error_value_high, Toast.LENGTH_LONG).show();
                    }

                    if (correction) {
                        String newStrValue = floatPreference ? Float.toString((float) value) : Integer.toString(Math.round((float) value));
                        //preference.getEditor().putString(preference.getKey(), newStrValue).apply();
                        preference.getSharedPreferences().edit().putString(preference.getKey(), newStrValue).apply();
                        editTextPreference.setText(newStrValue);
                        preference.setSummary(newStrValue);
                        return false;
                    }
                }
            }

            preference.setSummary(stringValue);
        } else {
            // For all other preferences, set the summary to the value's
            // simple string1 representation.
            preference.setSummary(stringValue);
        }
        return true;
    };

    static float[] getExtremums(String requestedKey) {


        float [] result = new float[] {0, 0};

        try {
            XmlPullParser xpp = context.getResources().getXml(R.xml.settings);

            xpp.next();
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
//				if (eventType == XmlPullParser.START_DOCUMENT) {
//					stringBuffer.append("--- Start XML ---");
//				} else
                if (eventType == XmlPullParser.START_TAG) {
                    //stringBuffer.append("\nSTART_TAG: " + xpp.getName());
                    if (xpp.getName().equals("EditTextPreference")) {
                        String curKey = xpp.getAttributeValue("http://schemas.android.com/apk/res/android", "key");
                        if (curKey != null) {
                            if (curKey.equals(requestedKey)) {
                                result[0] = StrToFloat(xpp.getAttributeValue(null, "min"), 0);
                                result[1] = StrToFloat(xpp.getAttributeValue(null, "max"), 0);
                                break;
                            }
                        }
                    }
                }
//				else if (eventType == XmlPullParser.END_TAG) {
//					stringBuffer.append("\nEND_TAG: " + xpp.getName());
//				} else if (eventType == XmlPullParser.TEXT) {
//					stringBuffer.append("\nTEXT: " + xpp.getText());
//				}
                //xpp.getAttributeValue("http://schemas.android.com/apk/res/android", "key")

                eventType = xpp.next();
            }
            //stringBuffer.append("\n--- End XML ---");
        }
        catch (Throwable t) {
            //Toast.makeText(context, "Request failed: "+t.toString(), Toast.LENGTH_LONG).show();
        }


        return result;

    }

    private static String getETAttribute(String circleRadius, String inputType) {
        return getAttribute("EditTextPreference", circleRadius, ANDROID_NAMESPACE, inputType, R.xml.settings);
    }

    static String getAttribute(String tag, String key, String namespace, String attribute, int resource) {
        try {
            XmlPullParser xpp = context.getResources().getXml(resource);

            xpp.next();
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals(tag)) {
                        String curKey = xpp.getAttributeValue(ANDROID_NAMESPACE, "key");
                        if (curKey != null) {
                            if (curKey.equals(key)) {
                                String result = xpp.getAttributeValue(namespace, attribute);
                                if (result != null)
                                    return result;
                                else return "";
                            }
                        }
                    }
                }
                eventType = xpp.next();
            }
        }
        catch (Throwable t) {
            //Toast.makeText(context, "Request failed: "+t.toString(), Toast.LENGTH_LONG).show();
        }

        return "";
    }

    public static float StrToFloat(String str1, float default_value) {
        if (str1 == null)
            return default_value;
        try {
            return Float.parseFloat(str1);
        } catch (NumberFormatException e) {
            Log.e("MyApp", "Error: " + e.getMessage());
            return default_value;
        }
    }

}