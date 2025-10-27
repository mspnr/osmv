# OSMv
![alt text](https://github.com/applikationsprogramvara/osmv/blob/main/app/src/main/res/mipmap-xxhdpi/ic_launcher3.png)

OSMv or Simple OSM Viewer is a very simple application that is using library [osmdroid](https://github.com/osmdroid/osmdroid) to display various maps.

This app is intended to be very simple and serve only one purpose: manual navigation using [Open Street Map](https://www.openstreetmap.org/) on Android.

<a href='https://play.google.com/store/apps/details?id=com.applikationsprogramvara.osmviewer'>
<img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'
width="240"/></a>

## Screenshots

<img src='https://lh3.googleusercontent.com/f3TOAL5VkbrQL1myBf4ayxqy1kKYiZw1pPU6xfhEoGBSy3T8FBrY-EtEdh3nxszLlbA=w2128-h1304' height='400'/> <img src='https://lh3.googleusercontent.com/5cjgCNK6y8gvpwV2d0QBveC9qusfbwInkIoMdZqec0lvfTjCnF1B4s57IG2okkxKQ1E4=w2128-h1304' height='400'/>

## Functions

 * Display the map (with dragging, zooming in and out)
 * Dedicated buttons for zoom in/out to show the map sharp
 * Show user position
 * Tracking user position (long click on show position)
 * Open a location using the app: click on a link with coordinated or address
 * Switch between metric and imperial units in settings
 
## Requested functions, ideas & plans

 * Add other maps - probably yes
 * Offline functions - probably yes
 * Search - probably yes
 * Turn-by-turn navigation - definitely not
 * Calculate gradient while moving
 * Tracking with fixing user not in the center of the screen

# Development

## Building the app

Building the app should be pretty straight forward: download the code, open it in Android Studio and build.

## Architecture

The app was started as an experiment, so all the main functions are packed inside `MainActivity`.

## Add-on

The app can be extended with an add-on in the developer version. It is not included to the standard distribution, though it is a field for experiments.
MapView is be transferred to the addon, so it can draw waypoints and routes. Placeholder is also transferred to the addon, so additional visual elements, like buttons, can be added.

# Misc

## Google Play Data Safety Declaration

This app declares the following data collection in the Google Play Data Safety form:

### Location Permissions Declared

**Approximate location** and **Precise location** - marked as:
- ☑️ Collected
- ☑️ Shared
- Purpose: App functionality only
- Optional: Users can deny location permissions

### Why These Permissions Are Declared

Although GPS coordinates stay on the device, **downloading map tiles inherently transmits location data**:

When the app displays a map region, it downloads tiles from third-party tile servers (OpenStreetMap, OpenTopoMap, etc.). Each tile request reveals which geographic area the user is viewing. Google classifies this as location data "collected and shared" with third parties.

### Data Security

**Encryption**: Not all data is encrypted in transit. The app offers both HTTPS and HTTP tile sources that users can switch between.

### Important Notes

- **No tracking**: The app does NOT use location data for analytics, advertising, or any purpose other than displaying maps
- **Optional**: Location permissions are optional - the app works without GPS (manual pan/zoom)
- **Local storage only**: All app settings are stored locally on your device. No user accounts, no server-side database
- **Data deletion**: Users can delete all app data by clearing app data in Android settings or uninstalling the app
