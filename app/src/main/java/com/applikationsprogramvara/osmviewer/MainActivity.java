package com.applikationsprogramvara.osmviewer;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.preference.PreferenceManager;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class MainActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_SETTINGS = 151;

    public static final OnlineTileSourceBase HTTP_MAPNIK = new XYTileSource("HttpMapnik",
            0, 19, 256, ".png", new String[]{
            "http://a.tile.openstreetmap.org/",
            "http://b.tile.openstreetmap.org/",
            "http://c.tile.openstreetmap.org/"}, "© OpenStreetMap contributors",
            new TileSourcePolicy(2,
                    TileSourcePolicy.FLAG_NO_BULK
                            | TileSourcePolicy.FLAG_NO_PREVENTIVE
                            | TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL
                            | TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED
            ));

    public static final OnlineTileSourceBase HTTP_OPENTOPOMAP = new XYTileSource("HttpOpenTopoMap",
            0, 17, 256, ".png", new String[]{
                    "http://a.tile.opentopomap.org/",
                    "http://b.tile.opentopomap.org/",
                    "http://c.tile.opentopomap.org/"},
            "Kartendaten: © OpenStreetMap-Mitwirkende, SRTM | Kartendarstellung: © OpenTopoMap (CC-BY-SA)");



    public static final OnlineTileSourceBase[] MAP_SOURCES_STD = new OnlineTileSourceBase[] {
            TileSourceFactory.MAPNIK,
            TileSourceFactory.OpenTopo
    };

    public static final OnlineTileSourceBase[] MAP_SOURCES_HTTP = new OnlineTileSourceBase[] {
            HTTP_MAPNIK,
            HTTP_OPENTOPOMAP
    };

    public static final int[] MAP_ICONS = new int[] {
            R.drawable.ic_map_usual,
            R.drawable.ic_map_topo
    };

    private static final int REQUEST_LOCATION_DISPLAY = 123;
    private static final int REQUEST_LOCATION_JUMP    = 124;

    public static final int ANIMATION_SPEED_FAST = 250;
    public static final int ANIMATION_SPEED_SLOW = 500;
    int mapSourceIndex;

    @BindView(R.id.map) MapView map;
    @BindView(R.id.tvDebugInfo) TextView tvDebugInfo;
    @BindView(R.id.tvZoom) TextView tvZoom;
    @BindView(R.id.tvSpeed) TextView tvSpeed;
    @BindView(R.id.tvAltitude) TextView tvAltitude;
    @BindView(R.id.btnJumpToLocation) ImageButton btnJumpToLocation;
    @BindView(R.id.btnGPS) ImageButton btnGPS;
    @BindView(R.id.btnChangeSource) ImageButton btnChangeSource;

    private EnhancedSharedPreferences prefs;
    private MyLocationNewOverlay mLocationOverlay;
    //private boolean followMode;
    private boolean showDebugInfo;
    private ScaleBarOverlay mScaleBarOverlay;
    private OnlineTileSourceBase[] mapSource;
    private UnitCalcInterface unitCalc;
    private GeoPoint requiredCenter;
    private boolean experimentalStickCenterOnZoom;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.ROOT); // yy-MM-dd HH:mm:ss

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //handle permissions first, before map is created. not depicted here

        //load/initialize the osmdroid configuration, this can be done
        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
        //setting this before the layout is inflated is a good idea
        //it 'should' ensure that the map has a writable location for the map cache, even without permissions
        //if no tiles are displayed, you can try overriding the cache path using Configuration.getInstance().setCachePath
        //see also StorageUtils
        //note, the load method also sets the HTTP User Agent to your application's package name, abusing osm's tile servers will get you banned based on this string

        //inflate and create the map
        setContentView(R.layout.activity_main);

        transparentStatusAndNavigation();


        ButterKnife.bind(this);
        prefs = EnhancedSharedPreferences.getDefaultSharedPreferences(this);




        map.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        //map.getZoomController().
        //map.setBuiltInZoomControls(false);
        map.setMultiTouchControls(true);
        //map.setTilesScaledToDpi(true);

        map.setTilesScaleFactor(getResources().getDisplayMetrics().density * .8f);


        mScaleBarOverlay = new ScaleBarOverlay(map);
        mScaleBarOverlay.setAlignBottom(true);
        mScaleBarOverlay.setAlignRight(true);

        loadSettings();


        if (!prefs.contains("mapPosX")) { //"InitialLocationJump")) {
            jumpToLocation();
            //prefs.edit().putBoolean("InitialLocationJump", true).apply();
        }

        IMapController mapController = map.getController();
        mapController.setZoom(prefs.getDouble("mapPosZ", 9.5));
        GeoPoint startPoint = new GeoPoint(prefs.getDouble("mapPosX", 48.8583), prefs.getDouble("mapPosY", 2.2944));
        mapController.setCenter(startPoint);

        findViewById(R.id.mainLayout).addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            mScaleBarOverlay.setScaleBarOffset(
                    (int) (getResources().getDisplayMetrics().density * (16 + 48 + (BuildConfig.DEBUG ? 48 : 0))) + v.getPaddingRight(),
                    (int) (getResources().getDisplayMetrics().density * 16) + v.getPaddingBottom()
            );
        });
        map.getOverlays().add(mScaleBarOverlay);

        map.getOverlays().add(0, new Overlay() {
            @Override
            public boolean onDoubleTap(MotionEvent e, MapView mapView) {
                resetRequiredCenter();
                return super.onDoubleTap(e, mapView);
            }
        });

        map.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                if (event.getX() != 0 || event.getY() != 0) // manual drag
                    resetRequiredCenter();
//                Log.d("MyApp3", "onScroll " + event.getX() + ", " + event.getY() );
                //changeFollowMode(false);
                printoutDebugInfo(null);
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                //requiredCenter = null;
                //changeFollowMode(false);
                //Log.d("MyApp3", "onZoom " + event.getZoomLevel());// + " " + event.getSource());
                printoutDebugInfo(null);
                return false;
            }
        });

        if (prefs.getBoolean("GPSisOn", false))
            switchGPS(); // switch back GPS if before exit or on rotate it was on

        tvDebugInfo.setText("");
        tvZoom.setText("");
        tvSpeed.setText("");


        setDebugInfo(prefs.getBoolean("ShowDebugInfo", false));

        if (!BuildConfig.DEBUG)
            findViewById(R.id.btnDebug).setVisibility(View.GONE);



        try { // dynamic loading of addon - available in debug only
            Class<?> addonClass = getClassLoader().loadClass(
                    "com.applikationsprogramvara.osmviewer_addon.Main");
            Method initAddon = addonClass.getDeclaredMethod(
                    "initAddon",
                    Activity.class, MapView.class, FrameLayout.class);

            FrameLayout placeholder1 = findViewById(R.id.placeholder1);
            initAddon.invoke(null, this, map, placeholder1);
        } catch (Exception e) {
            Log.e("MyApp", e.toString());
        }

        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction()))
            parseGeoUri(intent.getData());
    }

    private void loadSettings() {
        mapSourceIndex = prefs.getInt("mapSourceIndex", 0);
        mapSource = prefs.getBoolean("HttpSource", false) ? MAP_SOURCES_HTTP : MAP_SOURCES_STD;
        mapSetSource();

        switch (prefs.getString("UnitsOfMeasure", "metric")) {
            default:
            case "metric":
                unitCalc = new UnitCalcInterface() {
                    @Override
                    public double speed(double speed_m_s) {
                        return speed_m_s * 3600 / 1000; // km/h = kilometers / hour
                    }

                    @Override
                    public double altitude(double meters) {
                        return meters; // m = meters
                    }
                };
                mScaleBarOverlay.setUnitsOfMeasure(ScaleBarOverlay.UnitsOfMeasure.metric);
                break;
            case "imperial":
                unitCalc = new UnitCalcInterface() {
                    @Override
                    public double speed(double speed_m_s) {
                        return speed_m_s * 3600 / 1609.34; // mph = miles / hour
                    }

                    @Override
                    public double altitude(double meters) {
                        return meters * 3.2808f; // ft = feet
                    }
                };
                mScaleBarOverlay.setUnitsOfMeasure(ScaleBarOverlay.UnitsOfMeasure.imperial);
                break;
        }


        experimentalStickCenterOnZoom = prefs.getBoolean("ExperimentalStickCenterOnZoom", false);
    }

    private void printoutDebugInfo(Location l1) {

        btnJumpToLocation.setImageResource(followEnabled() ?
                R.drawable.ic_follow :
                R.drawable.ic_my_location
        );

        if (!showDebugInfo) return;

        tvZoom.setText(String.format(Locale.ROOT, "%.2f", map.getZoomLevelDouble()));

        String debugInfo = "";

        Location l2 = null;
        if (l1 != null)
            l2 = l1;
        else if (mLocationOverlay != null)
            l2 = mLocationOverlay.getLastFix();

        if (l2 != null)
            debugInfo = String.format(Locale.ROOT, "s %.2f, b %.2f, a %.2f\n%.6f; %.6f, %s\n",
                    //getBearing(l2), map.getBoundingBox().contains(l2.getLatitude(), l2.getLongitude())
                    l2.getSpeed(), l2.getBearing(), l2.getAltitude(),
                    l2.getLatitude(), l2.getLongitude(), sdf.format(new Date())
            );

        debugInfo += String.format(Locale.ROOT, "%.6f; %.6f (%.0f-%.0f)",
                map.getMapCenter().getLatitude(), map.getMapCenter().getLongitude(),
                map.getMinZoomLevel(), map.getMaxZoomLevel());
        tvDebugInfo.setText(debugInfo);

    }

    private boolean followEnabled() {
        return (mLocationOverlay != null) &&
                mLocationOverlay.isMyLocationEnabled() &&
                mLocationOverlay.isFollowLocationEnabled();
    }

    public void onResume(){
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up

        if (mLocationOverlay != null && map.getOverlays().contains(mLocationOverlay))
            setChangeLocationListener(); // restarting own implementation of location listener, otherwise arrow / person icon will be displayed correctly, but no interception will happen

    }

    private void setChangeLocationListener() {
        mLocationOverlay.getMyLocationProvider().startLocationProvider((location, source) -> { // own implementation of location listener in between map and overlay itself to intercept location changes
            if (followEnabled()) // following mode is on
                resetRequiredCenter();

//            Log.d("MyApp3", "onLocationChanged " + location.getSpeed() + " " + location.isFromMockProvider() + " " +
//                            location.getBearing() + " " + location.getAltitude() + " " + location.getLatitude() + " " + location.getLongitude() + " " +
//                    (mLocationOverlay != null ? mLocationOverlay.isMyLocationEnabled() : "") + " " +  (mLocationOverlay != null ? mLocationOverlay.isFollowLocationEnabled() : "")
//            );

            btnGPS.setImageResource(R.drawable.ic_gps_on);

            double speed = unitCalc.speed(location.getSpeed());
            if (speed == 0)
                tvSpeed.setText("");
            else if (speed < 1)
                tvSpeed.setText("s");
            else
                tvSpeed.setText(String.format(Locale.ROOT, "%.0f", speed));

            if (showDebugInfo)
                tvAltitude.setText(String.format(Locale.ROOT, "%.0f", unitCalc.altitude(location.getAltitude())));

            printoutDebugInfo(location);

//            if (followMode)
//                map.getController().setCenter(new GeoPoint(location.getLatitude(), location.getLongitude()));

            mLocationOverlay.onLocationChanged(location, source); // populate the current location to the location overlay, so the arrow / person icon can be placed correctly
        });
    }

    public void onPause(){
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up

        prefs.edit()
                .putDouble("mapPosX", map.getMapCenter().getLatitude())
                .putDouble("mapPosY", map.getMapCenter().getLongitude())
                .putDouble("mapPosZ", map.getZoomLevelDouble())
                .putInt("mapSourceIndex", mapSourceIndex)
                .apply();
    }


    @OnClick(R.id.btnChangeSource)
    void clickChangeSource() {
        mapSourceIndex++;
        if (mapSourceIndex >= mapSource.length)
            mapSourceIndex = 0;
        mapSetSource();
    }

    private void mapSetSource() {
        map.setTileSource(mapSource[mapSourceIndex]);

        // setting min/max zooms is not implemented inside mapview. pros: user can zoom in further with "pixel effect", cons: user is not aware of zoom limits
//        map.setMinZoomLevel((double) mapSource[mapSourceIndex].getMinimumZoomLevel());
//        map.setMaxZoomLevel((double) mapSource[mapSourceIndex].getMaximumZoomLevel()); // also setting lower max zoom cause harsh zoom out w/o animation
        btnChangeSource.setImageResource(MAP_ICONS[mapSourceIndex]);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case REQUEST_LOCATION_JUMP:
                    jumpToLocation();
                    break;
                case REQUEST_LOCATION_DISPLAY:
                    switchGPS();
                    break;
            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dlg_deny_loc_perm_title)
                    .setMessage(R.string.dlg_deny_loc_perm_message)
                    .setPositiveButton(R.string.btn_ok, null)
                    .show();
        }
    }


    private boolean permissionsGrantedAndLocationServiceEnabled(int requestID) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    requestID);
            return false;

        }

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        try { gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception e) { Log.e("MyApp", e.toString()); }

        try { networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception e) { Log.e("MyApp", e.toString()); }

        if (!gpsEnabled && !networkEnabled) {
            Toast.makeText(getBaseContext(), R.string.toast_location_service_disabled, Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    @OnClick(R.id.btnGPS)
    void switchGPS() {
        if ((mLocationOverlay == null) || !map.getOverlays().contains(mLocationOverlay)) {
            if (!permissionsGrantedAndLocationServiceEnabled(REQUEST_LOCATION_DISPLAY)) return;

            if (mLocationOverlay == null) {
                GpsMyLocationProvider myLocationProvider = new GpsMyLocationProvider(this);


                mLocationOverlay = new MyLocationNewOverlay(myLocationProvider, map);
                Bitmap person = getBitmap(this, R.drawable.ic_person);
                Bitmap arrow = getBitmap(this, R.drawable.ic_navigation);

                mLocationOverlay.setPersonHotspot(person.getWidth() / 2f, person.getHeight() / 2f);
                mLocationOverlay.setPersonIcon(person);

                mLocationOverlay.setDirectionArrow(person, arrow);
            }

            mLocationOverlay.enableMyLocation();
            setChangeLocationListener();
            map.getOverlays().add(mLocationOverlay);
            btnGPS.setImageResource(mLocationOverlay.getMyLocation() == null ?
                    R.drawable.ic_gps_search :
                    R.drawable.ic_gps_on
            );
            prefs.edit().putBoolean("GPSisOn", true).apply();
        } else {
            mLocationOverlay.disableMyLocation();
            mLocationOverlay.getMyLocationProvider().stopLocationProvider();
            map.getOverlays().remove(mLocationOverlay);
            btnGPS.setImageResource(R.drawable.ic_gps_off);
            btnJumpToLocation.setImageResource(R.drawable.ic_my_location);
            tvSpeed.setText("");
            prefs.edit().putBoolean("GPSisOn", false).apply();
        }
    }

    private double getBearing(Location location) {
        GeoPoint p1 = new GeoPoint(map.getMapCenter());
        GeoPoint p2 = new GeoPoint(location.getLatitude(), location.getLongitude());

        double v = p1.bearingTo(p2);
        //Log.d("MyApp2", "bearing = " + v);
        return v;
    }

    @OnClick(R.id.btnJumpToLocation)
    void jumpToLocation() {
        if ((mLocationOverlay != null) && map.getOverlays().contains(mLocationOverlay)) {
            GeoPoint myLocation = mLocationOverlay.getMyLocation();
            if ((myLocation != null) && (myLocation.getLatitude() != 0) && (myLocation.getLongitude() != 0)) {
                //Log.d("MyApp3", "jumpToLocation " + myLocation.getLatitude() + ";" + myLocation.getLongitude());
                jump(myLocation);
                return;
            }
        }

        if (!permissionsGrantedAndLocationServiceEnabled(REQUEST_LOCATION_JUMP)) return;

        boolean jumpPerformed = false;
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (lm != null) {
            List<String> providers = lm.getProviders(true); // "passive", "gps", "network"
            providers.add(0, lm.getBestProvider(new Criteria(), true)); // "gps"

            for(String provider: providers) {
                try {
                    @SuppressLint("MissingPermission") Location myLocation = lm.getLastKnownLocation(provider);
                    if (myLocation != null) {
                        //Log.d("MyApp3", "jumpToLocation " + myLocation.getLatitude() + ";" + myLocation.getLongitude());
                        jump(new GeoPoint(myLocation.getLatitude(), myLocation.getLongitude()));
                        jumpPerformed = true;
                        break;
                    }
                } catch (Exception ignored) { }
            }
        }

        if (!jumpPerformed)
            Toast.makeText(this, R.string.toast_loc_not_avail2, Toast.LENGTH_SHORT).show();

    }

    private void jump(GeoPoint targetPoint) {
        jump(targetPoint, 0);
    }

    private void jump(GeoPoint targetPoint, double requestedZoom) {
        setRequiredCenter(targetPoint);
        if (map.getWidth() == 0 || map.getHeight() == 0) { // no animation possible
            map.getController().setCenter(targetPoint);
            return;
        }

        GeoPoint startPoint = new GeoPoint(map.getMapCenter());
        double startZoom = map.getZoomLevelDouble();
        double targetZoom = requestedZoom == 0 ? startZoom : requestedZoom;

        List<GeoPoint> points = new ArrayList<>();
        points.add(startPoint);
        points.add(targetPoint);

        final BoundingBox boundingBox1 = BoundingBox.fromGeoPoints(points);
        double intermediateZoom = MapView.getTileSystem().getBoundingBoxZoom(boundingBox1, map.getWidth(), map.getHeight());
//        final GeoPoint intermediatePoint = boundingBox1.getCenterWithDateLine();
//        if (BuildConfig.DEBUG)
//            Toast.makeText(this, "Zooms " + startZoom + " " + intermediateZoom, Toast.LENGTH_SHORT).show();

        if (intermediateZoom < startZoom) {
            //Log.d("MyApp2", "zoom out animation start [" + startZoom + "] intermediate [" + intermediateZoom + "]");
            ValueAnimator zoomOut = ValueAnimator.ofFloat(0f, 1f);
            int speed = (startZoom - intermediateZoom < 5) ? ANIMATION_SPEED_FAST : ANIMATION_SPEED_SLOW;
            zoomOut.setDuration(speed);
            zoomOut.addUpdateListener(updatedAnimation -> {
                float fraction = updatedAnimation.getAnimatedFraction();
                try { // for an unknown reason here happens a crash in BoundingBox.set
                    map.getController().setZoom(startZoom + (intermediateZoom - startZoom) * fraction);
                } catch (Exception ignored) { }
            });
            zoomOut.setInterpolator(new AccelerateInterpolator());

            ValueAnimator move = ValueAnimator.ofFloat(0f, 1f);
            move.setDuration(ANIMATION_SPEED_SLOW);
            move.addUpdateListener(updatedAnimation -> {
                float fraction = updatedAnimation.getAnimatedFraction();
                map.getController().setCenter(new GeoPoint(
                        startPoint.getLatitude() + (targetPoint.getLatitude() - startPoint.getLatitude()) * fraction,
                        startPoint.getLongitude() + (targetPoint.getLongitude() - startPoint.getLongitude()) * fraction
                ));
            });
            move.setInterpolator(new AccelerateDecelerateInterpolator());


            ValueAnimator zoomIn = ValueAnimator.ofFloat(0f, 1f);
            zoomIn.setDuration(speed);
            zoomIn.addUpdateListener(updatedAnimation -> {
                float fraction = updatedAnimation.getAnimatedFraction();
                map.getController().setZoom(intermediateZoom + (targetZoom - intermediateZoom) * fraction);
                map.getController().setCenter(targetPoint); // important

//                if (fraction == 1)
//                    Log.d("MyApp3", "jumpToLocation end zoom in " + map.getMapCenter().getLatitude() + ";" + map.getMapCenter().getLongitude());
            });
            zoomOut.setInterpolator(new DecelerateInterpolator());

            AnimatorSet animation = new AnimatorSet();
            animation.playSequentially(zoomOut, move, zoomIn);
            animation.start();
        } else {

            ValueAnimator move = ValueAnimator.ofFloat(0f, 1f);
            move.setDuration(ANIMATION_SPEED_SLOW);
            move.addUpdateListener(updatedAnimation -> {
                float fraction = updatedAnimation.getAnimatedFraction();
                map.getController().setCenter(new GeoPoint(
                        startPoint.getLatitude() + (targetPoint.getLatitude() - startPoint.getLatitude()) * fraction,
                        startPoint.getLongitude() + (targetPoint.getLongitude() - startPoint.getLongitude()) * fraction
                ));
                if (targetZoom != startZoom)
                    map.getController().setZoom(startZoom + (targetZoom - startZoom) * fraction);
//            if (fraction == 1)
//                Log.d("MyApp3", "jumpToLocation end move " + map.getMapCenter().getLatitude() + ";" + map.getMapCenter().getLongitude());
            });
            move.setInterpolator(new AccelerateDecelerateInterpolator());

            move.start();
        }
    }

    private void resetRequiredCenter() {
        requiredCenter = null;
//        Log.d("MyApp3", "resetRequiredCenter");
    }

    private void setRequiredCenter(GeoPoint newCenter) {
        requiredCenter = new GeoPoint(newCenter);
//        Log.d("MyApp3", "setRequiredCenter " + newCenter.getLatitude() + " " + newCenter.getLongitude());
    }

    @OnLongClick(R.id.btnJumpToLocation)
    boolean followMode() {
        //followMode = b;
        //Toast.makeText(this, "followMode " + followMode, Toast.LENGTH_LONG).show();

        if ((mLocationOverlay != null) && mLocationOverlay.isMyLocationEnabled()) {
            resetRequiredCenter();
            if (!mLocationOverlay.isFollowLocationEnabled())
                mLocationOverlay.enableFollowLocation();
            else mLocationOverlay.disableFollowLocation();

            if (mLocationOverlay.isFollowLocationEnabled()) {
                btnJumpToLocation.setImageResource(R.drawable.ic_follow);
                Toast.makeText(this, R.string.toast_follow_loc_1, Toast.LENGTH_SHORT).show();
            } else {
                btnJumpToLocation.setImageResource(R.drawable.ic_my_location);
                Toast.makeText(this, R.string.toast_follow_loc_0, Toast.LENGTH_SHORT).show();
            }
        }


        return true;
    }

    private static Bitmap getBitmap(Context context, int resID) {
        Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), resID, null);

        Bitmap bitmap;
        try {
            bitmap = Bitmap.createBitmap(
                    drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (OutOfMemoryError e) {
            // Handle the error
            return null;
        }
    }

    @OnClick(R.id.btnZoomOut)
    void clickZoomOut() {
        changeZoom(-1);
    }

    @OnClick(R.id.btnZoomIn)
    void clickZoomIn() {
        changeZoom(+1);
    }

    private void changeZoom(double requestedDiff) {
        if (requiredCenter == null)
            setRequiredCenter(new GeoPoint(map.getMapCenter()));  // fix map center if it is not yet fixed

//        Log.d("MyApp3", "animation start " +  map.getMapCenter().getLatitude() + "; " + map.getMapCenter().getLongitude() + " / (" + requestedDiff + ") " + map.getZoomLevelDouble());
        double startZoom = map.getZoomLevelDouble();
        double targetZoom;
        if (map.getZoomLevelDouble() != Math.round(map.getZoomLevelDouble())) {
            if (requestedDiff > 0)
                targetZoom = Math.ceil(map.getZoomLevelDouble());
            else
                targetZoom = Math.floor(map.getZoomLevelDouble());
        } else targetZoom = Math.round(map.getZoomLevelDouble() + requestedDiff);

//        map.getController().zoomTo(targetZoom, ANIMATION_SPEED_FAST);


        ValueAnimator animation1 = ValueAnimator.ofFloat(0f, 1f);
        animation1.setDuration(ANIMATION_SPEED_FAST);
        animation1.addUpdateListener(updatedAnimation -> {
            float fraction = updatedAnimation.getAnimatedFraction();
            map.getController().setZoom(startZoom + (targetZoom - startZoom) * fraction);
            if (experimentalStickCenterOnZoom && requiredCenter != null)
                map.getController().setCenter(requiredCenter);

//            if (fraction == 1)
//                Log.d("MyApp3", "animation end    " + map.getMapCenter().getLatitude() + "; " + map.getMapCenter().getLongitude() + " / (" + requestedDiff + ") " + map.getZoomLevelDouble());
        });
        animation1.start();

        //map.getController().setZoom(targetZoom);
    }

    @OnLongClick(R.id.btnDebug)
    boolean testclick() {

//        Convert latitude, longitude to spherical mercator x, y.
//        Get distance between your two points in spherical mercator.
//                The equator is about 40m meters long projected and tiles are 256 pixels wide, so the pixel length of that map at a given zoom level is about 256 * distance/40000000 * 2^zoom. Try zoom=0, zoom=1, zoom=2 until the distance is too long for the pixel dimensions of your map.

//        Bounds2 mapArea, float paddingFactor)
//
//            double ry1 = Math.Log((Math.Sin(GeometryUtils.Deg2Rad(mapArea.MinY)) + 1)
//                    / Math.Cos(GeometryUtils.Deg2Rad(mapArea.MinY)));
//            double ry2 = Math.Log((Math.Sin(GeometryUtils.Deg2Rad(mapArea.MaxY)) + 1)
//                    / Math.Cos(GeometryUtils.Deg2Rad(mapArea.MaxY)));
//            double ryc = (ry1 + ry2) / 2;
//            double centerY = GeometryUtils.Rad2Deg(Math.Atan(Math.Sinh(ryc)));
//
//            double resolutionHorizontal = mapArea.DeltaX / Viewport.Width;
//
//            double vy0 = Math.Log(Math.Tan(Math.PI*(0.25 + centerY/360)));
//            double vy1 = Math.Log(Math.Tan(Math.PI*(0.25 + mapArea.MaxY/360)));
//            double viewHeightHalf = Viewport.Height/2.0f;
//            double zoomFactorPowered = viewHeightHalf
//                    / (40.7436654315252*(vy1 - vy0));
//            double resolutionVertical = 360.0 / (zoomFactorPowered * 256);
//
//            double resolution = Math.Max(resolutionHorizontal, resolutionVertical)
//                    * paddingFactor;
//            double zoom = Math.Log(360 / (resolution * 256), 2);
//            double lon = mapArea.Center.X;
//            double lat = centerY;
//
//            CenterMapOnPoint(new PointD2(lon, lat), zoom);

//        BoundingBox bb = map.getBoundingBox();
//
//        Toast.makeText(this, String.format(Locale.ROOT,
//                "[%.2f] zoom\n%.8f lat\n%.8f lon ",
//                map.getZoomLevelDouble(),
//                bb.getLatNorth() - bb.getLatSouth(),
//                bb.getLonEast() - bb.getLonWest()
//        ), Toast.LENGTH_LONG).show();

//        jump(new GeoPoint(
//                // 48.8583, 2.2944 // Paris
//                -85d, 145d // South Pacific
//        ));

        DEBUGlocationDialog();

        return true;
    }


    void changeCoordinates(GeoPoint target) {
        double startLat = map.getMapCenter().getLatitude();
        double startLong = map.getMapCenter().getLongitude();
        double targetLat = target.getLatitude();
        double targetLong = target.getLongitude();


        ValueAnimator animation1 = ValueAnimator.ofFloat(0f, 1f);
        animation1.setDuration(250);
        animation1.addUpdateListener(updatedAnimation -> {
            float fraction = updatedAnimation.getAnimatedFraction();
            //map.getController().setZoom(startZoom + difference * fraction);
            map.getController().setCenter(new GeoPoint(
                    startLat + (targetLat - startLat) * fraction,
                    startLong + (targetLong - startLong) * fraction
            ));
        });
        animation1.start();
    }

    private void transparentStatusAndNavigation() {
        //make full transparent statusBar
        if (Build.VERSION.SDK_INT >= 19 && Build.VERSION.SDK_INT < 21) {
            setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, true);
        }
        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
//                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }

        //Build.VERSION_CODES.LOLLIPOP
        if (Build.VERSION.SDK_INT >= 21) {
            setWindowFlag(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, false);
//            getWindow().setStatusBarColor(Color.TRANSPARENT);
//            getWindow().setNavigationBarColor(Color.TRANSPARENT);


            getWindow().setStatusBarColor(getResources().getColor(R.color.btnBack));
            getWindow().setNavigationBarColor(getResources().getColor(R.color.btnBack));
        }


    }

    private void setWindowFlag(final int bits, boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    @OnClick(R.id.btnSettings)
    void openSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class), REQUEST_CODE_SETTINGS);
    }

    @OnClick(R.id.btnDebug)
    void showDebugInfo() {
        setDebugInfo(!showDebugInfo);
    }

    private void setDebugInfo(boolean b) {
        showDebugInfo = b;
        for (View v: new View[] {tvDebugInfo, findViewById(R.id.placeholder1), tvZoom, tvAltitude})
            v.setVisibility(showDebugInfo ? View.VISIBLE : View.GONE);

        prefs.edit().putBoolean("ShowDebugInfo", showDebugInfo).apply();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SETTINGS:
                    //recreate(); // dirty solution
                    loadSettings();
                    map.invalidate();
                    break;
            }
        }
    }

    interface UnitCalcInterface {
        double speed(double speed_m_s);
        double altitude(double meters);
    }


    void DEBUGlocationDialog() {

        final EditText input = new EditText(this);

        input.setText(prefs.getString("locationName", "Paris"));

        new AlertDialog.Builder(this)
                .setTitle("Address")
                .setView(input)
                .setPositiveButton("OK", (d, w) -> {
                    String locationName = input.getText().toString();
                    prefs.edit().putString("locationName", locationName).apply();
                    searchLocation(locationName);
                })
                .setNegativeButton("Cancel", (dialog, w) -> dialog.cancel())
                .show();
    }


    void searchLocation(String locationName ) {

        AsyncTask.execute(() -> {
            GeocoderNominatim coderNominatim = new GeocoderNominatim(getPackageName()); // com.applikationsprogramvara.osmviewer

            try {
                List<Address> geoResults = coderNominatim.getFromLocationName(locationName, 1);

                if (geoResults.size() == 0) {
                    runOnUiThread(() -> Toast.makeText(this, "Location \"" + locationName + "\" is not found", Toast.LENGTH_LONG).show());
                } else {
                    Address address = geoResults.get(0);
                    Bundle extras = address.getExtras();
                    BoundingBox bb1 = extras.getParcelable("boundingbox"); // east and west is always given or interpreted vice versa
                    BoundingBox bb2 = new BoundingBox(bb1.getLatNorth(), bb1.getLonWest(), bb1.getLatSouth(), bb1.getLonEast());
                    runOnUiThread(() -> {
                        // option 1: just jump to the center of the object
                        jump(new GeoPoint(address.getLatitude(), address.getLongitude()));
                        // option 2: zoom to object borders
//                        resetRequiredCenter();
//                        map.zoomToBoundingBox(bb2, false);
                        Toast.makeText(this, "Navigating to \"" + locationName + "\"", Toast.LENGTH_LONG).show();
                    });

                    // example of returned bounding box
                    // Bundle[{
                    // osm_type=relation,
                    // boundingbox=N:48.902156; E:2.224122; S:48.8155755; W:2.4697602,
                    // osm_id=7444,
                    // display_name=Paris, Ile-de-France, Metropolitan France, France
                    // }]

                    // analogue is calling the URL in browser
                    // https://nominatim.openstreetmap.org/search?format=json&accept-language=de&addressdetails=1&limit=1&q=Paris
                    // https://nominatim.openstreetmap.org/search?format=json&limit=1&q=Paris

                    // json result from nominatim
                    // [
                    //  {
                    //    "place_id": 234423737,
                    //    "licence": "Data © OpenStreetMap contributors, ODbL 1.0. https://osm.org/copyright",
                    //    "osm_type": "relation",
                    //    "osm_id": 7444,
                    //    "boundingbox": [
                    //      "48.8155755",
                    //      "48.902156",
                    //      "2.224122",
                    //      "2.4697602"
                    //    ],
                    //    "lat": "48.8566969",
                    //    "lon": "2.3514616",
                    //    "display_name": "Paris, Ile-de-France, Metropolitan France, France",
                    //    "class": "boundary",
                    //    "type": "administrative",
                    //    "importance": 0.9417101715588673,
                    //    "icon": "https://nominatim.openstreetmap.org/images/mapicons/poi_boundary_administrative.p.20.png"
                    //  }
                    //]
                }
            } catch (IOException e) {
                Log.e("MyApp", e.toString());
                runOnUiThread(() -> Toast.makeText(this, "Error on searching location", Toast.LENGTH_LONG).show());
            }


        });

    }

    private void parseGeoUri(Uri uri) {
        // Extract of possible options from
        // https://developer.android.com/guide/components/intents-common#Maps

        // 1 Coordinates simple
        // geo:latitude,longitude
        //   Show the map at the given longitude and latitude.
        //    Example: "geo:47.6,-122.3"

        // 2 Coordinates with zoom
        //  geo:latitude,longitude?z=zoom
        //    Show the map at the given longitude and latitude at a certain zoom level. A zoom level of 1 shows the whole Earth, centered at the given lat,lng. The highest (closest) zoom level is 23.
        //    Example: "geo:47.6,-122.3?z=11"

        // 3 Coordinates with label
        //  geo:0,0?q=lat,lng(label)
        //    Show the map at the given longitude and latitude with a string label.
        //    Example: "geo:0,0?q=34.99,-106.61(Treasure)"

        //  4 Address
        //  geo:0,0?q=my+street+address
        //    Show the location for "my street address" (may be a specific address or location query).
        //    Example: "geo:0,0?q=1600+Amphitheatre+Parkway%2C+CA"

        // parser is based on https://github.com/osmandapp/Osmand/blob/master/OsmAnd-java/src/main/java/net/osmand/util/GeoPointParserUtil.java

        if (!"geo".equals(uri.getScheme()))
            return;

        String schemeSpecific = uri.getSchemeSpecificPart();
        if (schemeSpecific == null)
            return;

        String name = null;
        final Pattern namePattern = Pattern.compile("[\\+\\s]*\\((.*)\\)[\\+\\s]*$");
        final Matcher nameMatcher = namePattern.matcher(schemeSpecific);
        if (nameMatcher.find()) {
            name = URLDecoder.decode(nameMatcher.group(1));
            if (name != null)
                schemeSpecific = schemeSpecific.substring(0, nameMatcher.start());
        }

        String positionPart;
        String queryPart = "";
        int queryStartIndex = schemeSpecific.indexOf('?');
        if (queryStartIndex == -1) {
            positionPart = schemeSpecific;
        } else {
            positionPart = schemeSpecific.substring(0, queryStartIndex);
            if (queryStartIndex < schemeSpecific.length())
                queryPart = schemeSpecific.substring(queryStartIndex + 1);
        }

        final Pattern positionPattern = Pattern.compile(
                "([+-]?\\d+(?:\\.\\d+)?),\\s?([+-]?\\d+(?:\\.\\d+)?)");
        final Matcher positionMatcher = positionPattern.matcher(positionPart);
        double lat = 0.0;
        double lon = 0.0;
        if (positionMatcher.find()) {
            lat = Double.valueOf(positionMatcher.group(1));
            lon = Double.valueOf(positionMatcher.group(2));
        }

        int zoom = -1; // NO_ZOOM
        String searchRequest = null;
        for (String param : queryPart.split("&")) {
            String paramName;
            String paramValue = null;
            int nameValueDelimititerIndex = param.indexOf('=');
            if (nameValueDelimititerIndex == -1) {
                paramName = param;
            } else {
                paramName = param.substring(0, nameValueDelimititerIndex);
                if (nameValueDelimititerIndex < param.length())
                    paramValue = param.substring(nameValueDelimititerIndex + 1);
            }

            if ("z".equals(paramName) && paramValue != null) {
                zoom = Integer.parseInt(paramValue);
            } else if ("q".equals(paramName) && paramValue != null) {
                searchRequest = URLDecoder.decode(paramValue);
            }
        }

        // what is it part for? "1600 Amphitheatre Parkway", CA is working all right, but "Unter den Linden 6, 10117 Berlin" is not working
//        if (searchRequest != null) {
//            final Matcher positionInSearchRequestMatcher =
//                    positionPattern.matcher(searchRequest);
//            if (lat == 0.0 && lon == 0.0 && positionInSearchRequestMatcher.find()) {
//                lat = Double.valueOf(positionInSearchRequestMatcher.group(1));
//                lon = Double.valueOf(positionInSearchRequestMatcher.group(2));
//            }
//        }

        if (lat == 0.0 && lon == 0.0 && searchRequest != null) {
            //Log.d("MyApp", "Address \"" + searchRequest + "\"");
            searchLocation(searchRequest);
            return;
        }

        if (zoom != -1) { // NO_ZOOM
            //Log.d("MyApp", "Coordinates with zoom " + lat + " " + lon + " " +  zoom + " " + name);
//            resetRequiredCenter();
//            map.getController().setCenter(new GeoPoint(lat, lon));
//            map.getController().setZoom((double) zoom);
            GeoPoint targetPoint = new GeoPoint(lat, lon);
            int targetZoom = zoom;
            map.post(() -> jump(targetPoint, targetZoom));
            Toast.makeText(this, "Navigating to [" + lat + ", " + lon + "] with zoom " + zoom, Toast.LENGTH_LONG).show();
            return;
        }
        //Log.d("MyApp", "Coordinates simple " + lat + " " + lon + " " + name);
        GeoPoint targetPoint = new GeoPoint(lat, lon);
        map.post(() -> jump(targetPoint));
//        resetRequiredCenter();
//        map.getController().setCenter(new GeoPoint(lat, lon));
        Toast.makeText(this, "Navigating to [" + lat + ", " + lon + "]", Toast.LENGTH_LONG).show();
    }


}
