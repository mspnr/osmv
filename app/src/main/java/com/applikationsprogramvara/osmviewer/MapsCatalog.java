package com.applikationsprogramvara.osmviewer;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.tilesource.TileSourcePolicy;
import org.osmdroid.tileprovider.tilesource.XYTileSource;

public class MapsCatalog {

    // copy of TileSourceFactory.MAPNIK with adjusted name
    private static final OnlineTileSourceBase MAPNIK = new XYTileSource("OpenStreetMap",
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

    private static final OnlineTileSourceBase MAPNIK_HTTP = new XYTileSource("OpenStreetMap http",
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

    private static final OnlineTileSourceBase HTTP_OPENTOPOMAP = new XYTileSource("OpenTopoMap http",
            0, 17, 256, ".png", new String[]{
                    "http://a.tile.opentopomap.org/",
                    "http://b.tile.opentopomap.org/",
                    "http://c.tile.opentopomap.org/"},
            "Kartendaten: © OpenStreetMap-Mitwirkende, SRTM | Kartendarstellung: © OpenTopoMap (CC-BY-SA)");

    private static final OnlineTileSourceBase HIKEBIKEMAP = new XYTileSource("HikeBikeMap",
            0, 19, 256, ".png",
            new String[] { "https://tiles.wmflabs.org/hikebike/"  });

    private static final OnlineTileSourceBase[][] MAP_SOURCES = new OnlineTileSourceBase[][] {
            {
//                TileSourceFactory.MAPNIK,
                MAPNIK,
                MAPNIK_HTTP,
                HIKEBIKEMAP,
            }, {
                TileSourceFactory.OpenTopo,
                HTTP_OPENTOPOMAP,
            }
    };

    private static final int[] MAP_ICONS = new int[] {
            R.drawable.ic_map_usual,
            R.drawable.ic_map_topo
    };

    private final SharedPreferences prefs;
    private final CallbackSetMapSource setMap;
    private int mapIndex1;
    private int mapIndex2;

    public MapsCatalog(SharedPreferences prefs, @Nullable CallbackSetMapSource setMap) {
        this.prefs = prefs;
        this.setMap = setMap;
    }

    public void load() {
        mapIndex1 = prefs.getInt("mapSourceIndex", 0);
        load(mapIndex1);
    }

    public void load(int index1) {
        mapIndex2 = prefs.getInt("mapSourceIndex2_" + index1, 0);
        mapSetSource(index1);
    }

    public void nextMap() {
        mapIndex1++;
        if (mapIndex1 >= MAP_SOURCES.length)
            mapIndex1 = 0;

        prefs.edit().putInt("mapSourceIndex", mapIndex1).apply();
        load(mapIndex1);
    }

    public void selectSource(Context context) {
        if (mapIndex1 >= MAP_SOURCES.length)
            mapIndex1 = 0;

        selectSource(context, mapIndex1);
    }

    public void selectSource(Context context, int index1) {
        MenuUtils.MenuBuilder menu = new MenuUtils.MenuBuilder(context)
                .setTitle(R.string.dlg_select_map_source_title);

        for (int i = 0; i < MAP_SOURCES[index1].length; i++) {
            int i2 = i;
            menu.add(MAP_SOURCES[index1][i].toString(), () -> {
                mapIndex2 = i2;
                prefs.edit().putInt("mapSourceIndex2_" + index1, mapIndex2).apply();
                mapSetSource(index1);
            });
        }

        menu.show2(mapIndex2);
    }

    private void mapSetSource(int index1) {
        // setting min/max zooms is not implemented inside mapview. pros: user can zoom in further with "pixel effect", cons: user is not aware of zoom limits
//        map.setMinZoomLevel((double) mapSource[mapSourceIndex].getMinimumZoomLevel());
//        map.setMaxZoomLevel((double) mapSource[mapSourceIndex].getMaximumZoomLevel()); // also setting lower max zoom cause harsh zoom out w/o animation

        if (index1 >= MAP_SOURCES.length)
            index1 = 0;
        if (mapIndex2 >= MAP_SOURCES[index1].length)
            mapIndex2 = 0;

        if (setMap != null)
        setMap.set(
                MAP_SOURCES[index1][mapIndex2],
                MapsCatalog.MAP_ICONS[index1]
        );

    }

    public interface CallbackSetMapSource {
        void set(ITileSource source, int image);
    }

}
