package com.applikationsprogramvara.osmviewer;

import android.content.Context;

import androidx.annotation.NonNull;

import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy;
import org.osmdroid.tileprovider.tilesource.XYTileSource;

public class MapsCatalog {

    // copy of TileSourceFactory.MAPNIK;
    public static final OnlineTileSourceBase MAPNIK = new XYTileSource("OpenStreetMap",
            0, 19, 256, ".png", new String[] {
                    "https://a.tile.openstreetmap.org/",
                    "https://b.tile.openstreetmap.org/",
                    "https://c.tile.openstreetmap.org/" },"© OpenStreetMap contributors",
            new TileSourcePolicy(2,
                    TileSourcePolicy.FLAG_NO_BULK
                    | TileSourcePolicy.FLAG_NO_PREVENTIVE
                    | TileSourcePolicy.FLAG_USER_AGENT_MEANINGFUL
                    | TileSourcePolicy.FLAG_USER_AGENT_NORMALIZED
            ));

    public static final OnlineTileSourceBase MAPNIK_HTTP = new XYTileSource("OpenStreetMap http",
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

    public static final OnlineTileSourceBase HTTP_OPENTOPOMAP = new XYTileSource("OpenTopoMap http",
            0, 17, 256, ".png", new String[]{
                    "http://a.tile.opentopomap.org/",
                    "http://b.tile.opentopomap.org/",
                    "http://c.tile.opentopomap.org/"},
            "Kartendaten: © OpenStreetMap-Mitwirkende, SRTM | Kartendarstellung: © OpenTopoMap (CC-BY-SA)");

    public static final OnlineTileSourceBase HIKEBIKEMAP = new XYTileSource("HikeBikeMap",
            0, 19, 256, ".png",
            new String[] { "https://tiles.wmflabs.org/hikebike/"  });

    public static final OnlineTileSourceBase[][] MAP_SOURCES = new OnlineTileSourceBase[][] {
            {
                MAPNIK,
                MAPNIK_HTTP,
                HIKEBIKEMAP,
            }, {
                TileSourceFactory.OpenTopo,
                HTTP_OPENTOPOMAP,
            }
    };

    public static final int[] MAP_ICONS = new int[] {
            R.drawable.ic_map_usual,
            R.drawable.ic_map_topo
    };

    public static final OnlineTileSourceBase WAY_MARKED_TRAILS_HIKING = new XYTileSource("WayMarkedTrails Hiking",
            0, 18, 256, ".png",
            new String[] { "https://tile.waymarkedtrails.org/hiking/" });

    public static final OnlineTileSourceBase WAY_MARKED_TRAILS_HIKING_HTTP = new XYTileSource("WayMarkedTrails Hiking http",
            0, 18, 256, ".png",
            new String[] { "https://tile.waymarkedtrails.org/hiking/" });

    public static final OnlineTileSourceBase WAY_MARKED_TRAILS_CYCLING = new XYTileSource("WayMarkedTrails Cycling",
            0, 18, 256, ".png",
            new String[] { "https://tile.waymarkedtrails.org/cycling/" });

    public static final OnlineTileSourceBase WAY_MARKED_TRAILS_CYCLING_HTTP = new XYTileSource("WayMarkedTrails Cycling http",
            0, 18, 256, ".png",
            new String[] { "https://tile.waymarkedtrails.org/cycling/" });

    public static final OnlineTileSourceBase[] OVERLAY_SOURCES = new OnlineTileSourceBase[] {
            WAY_MARKED_TRAILS_HIKING,
            WAY_MARKED_TRAILS_HIKING_HTTP,
            WAY_MARKED_TRAILS_CYCLING,
            WAY_MARKED_TRAILS_CYCLING_HTTP,
    };

    private final EnhancedSharedPreferences prefs;
    private final CallbackSetMapSource setMap;
    private final CallbackSetOverlaySource setOverlay;
    private int mapIndex1;
    private int mapIndex2;
    private int overlayIndex;

    public MapsCatalog(EnhancedSharedPreferences prefs, @NonNull CallbackSetMapSource setMap, @NonNull CallbackSetOverlaySource setOverlay) {
        this.prefs = prefs;
        this.setMap = setMap;
        this.setOverlay = setOverlay;
    }

    public void load() {
        mapIndex1 = prefs.getInt("mapSourceIndex", 0);
        mapIndex2 = prefs.getInt("mapSourceIndex2_" + mapIndex1, 0);
        overlayIndex = prefs.getInt("overlaySourceIndex", 0);
        mapSetSource();
        setOverlay(prefs.getBoolean("OverlayDisplayed", false));
    }

    public void nextMap() {
        mapIndex1++;
        if (mapIndex1 >= MAP_SOURCES.length)
            mapIndex1 = 0;

        prefs.edit().putInt("mapSourceIndex", mapIndex1).apply();
        mapIndex2 = prefs.getInt("mapSourceIndex2_" + mapIndex1, 0);

        mapSetSource();
    }

    public void selectSource(Context context) {
        if (mapIndex1 >= MAP_SOURCES.length)
            mapIndex1 = 0;

        MenuUtils.MenuBuilder menu = new MenuUtils.MenuBuilder(context)
                .setTitle(R.string.dlg_select_map_source_title);

        for (int i = 0; i < MAP_SOURCES[mapIndex1].length; i++) {
            int i2 = i;
            menu.add(MAP_SOURCES[mapIndex1][i].toString(), () -> {
                mapIndex2 = i2;
                prefs.edit().putInt("mapSourceIndex2_" + mapIndex1, mapIndex2).apply();
                mapSetSource();
            });
        }

        menu.show2(mapIndex2);
    }

    private void mapSetSource() {
        // setting min/max zooms is not implemented inside mapview. pros: user can zoom in further with "pixel effect", cons: user is not aware of zoom limits
//        map.setMinZoomLevel((double) mapSource[mapSourceIndex].getMinimumZoomLevel());
//        map.setMaxZoomLevel((double) mapSource[mapSourceIndex].getMaximumZoomLevel()); // also setting lower max zoom cause harsh zoom out w/o animation

        if (mapIndex1 >= MAP_SOURCES.length)
            mapIndex1 = 0;
        if (mapIndex2 >= MAP_SOURCES[mapIndex1].length)
            mapIndex2 = 0;

        setMap.set(
                MAP_SOURCES[mapIndex1][mapIndex2],
                MapsCatalog.MAP_ICONS[mapIndex1]
        );

    }

    public interface CallbackSetMapSource {
        void set(ITileSource source, int image);
    }

    public void setOverlay(boolean show) {
        if (overlayIndex >= OVERLAY_SOURCES.length)
            overlayIndex = 0;
        setOverlay.set(show, OVERLAY_SOURCES[overlayIndex]);
        
        prefs.edit().putBoolean("OverlayDisplayed", show).apply();
    }

    public void selectOverlay(Context context) {
        MenuUtils.MenuBuilder menu = new MenuUtils.MenuBuilder(context)
                .setTitle(R.string.dlg_select_overlay_source_title);

        for (int i = 0; i < OVERLAY_SOURCES.length; i++) {
            int i2 = i;
            menu.add(OVERLAY_SOURCES[i].toString(), () -> {
                overlayIndex = i2;
                prefs.edit().putInt("overlaySourceIndex", overlayIndex).apply();
                setOverlay(true);
            });
        }

        menu.show2(overlayIndex);
    }

    public interface CallbackSetOverlaySource {
        void set(boolean show, ITileSource source);
    }


}
