package jujumap.juju;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.*;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.*;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class JujuMap extends Activity implements LocationListener, SharedPreferences.OnSharedPreferenceChangeListener {

    String   osmDir              = "/osmdroid";
    String   tourDir             = osmDir + "/historia-viva/";
    String   trackfile           = "poitrack.kml";
    String   prefDownloadUrl     = "http://www.historia-viva.net/downloads/";
    String   prefTourName        = "";
    String   prefCountryCode     = "";
    String   prefLocale          = "default";
    GeoPoint prefCurrentLocation = new GeoPoint(49.598,11.005);
    Boolean  prefShowAlarm       = false;
    Boolean  prefShowPois        = true;
    Boolean  prefShowMetrics     = true;
    Boolean  autoZoom            = false;
    int      prefZoomLevel       = 12;
    int      prefAutoZoomLevel   = 16;
    double   prefAlarmDist       = 60.0;

    MapView        mapView;
    IMapController mapController;

    SimpleLocationOverlay locationOverlay;     // holds GPS-location
    PathOverlay           track_kml_Overlay;   // holds poitrack
    ItemizedIconOverlay   poi_kml_Overlay;     // holds POIs

    Track track_kml = new Track();
    POIs  pois_kml  = new POIs();

    File sdcard;
    String  poiMapping = "";
    BoundingBoxE6 tour_bBox;
    SharedPreferences settings;

    AlertDialog.Builder singleTapAlert;
    AlertDialog.Builder twoPressAlert;
    AlertDialog.Builder proximityAlert;

    public static HashMap <String, String> tour_file2text;
    public static HashMap <String, String> tour_text2file;

    @Override
    public void onCreate (Bundle savedInstanceState) {

        super.onCreate (savedInstanceState);

        JujuMap.tour_file2text = new HashMap <String, String>();
        JujuMap.tour_text2file = new HashMap <String, String>();

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        settings.registerOnSharedPreferenceChangeListener(this);

        initOsmdroid();

        prefTourName    = settings.getString  ("prefTourName"    , prefTourName   );
        prefCountryCode = settings.getString  ("prefCountryCode" , prefCountryCode);
        prefShowPois    = settings.getBoolean ("prefShowPois"    , prefShowPois   );
        prefShowMetrics = settings.getBoolean ("prefShowMetrics" , prefShowMetrics);
        prefShowAlarm   = settings.getBoolean ("prefShowAlarm"   , prefShowAlarm  );
        prefLocale      = settings.getString  ("prefLocale"      , prefLocale     );

        if (prefTourName.equals("")) {

            Intent intent = new Intent (this, TourListOffline.class);

            intent.putExtra("osmpath", new File(sdcard, osmDir).toString());
            intent.putExtra("path"   , new File(sdcard, tourDir).toString());
            intent.putExtra("url"    , prefDownloadUrl);

            startActivityForResult (intent, 1234);

        } else {

            loadKML();

            mapView.getOverlays().add(track_kml_Overlay);

            if (prefShowPois) mapView.getOverlays().add(poi_kml_Overlay);

            prefZoomLevel = settings.getInt("prefZoomLevel", prefZoomLevel);

            prefCurrentLocation.setLatitudeE6 (settings.getInt("latitudeE6" , prefCurrentLocation.getLatitudeE6()));
            prefCurrentLocation.setLongitudeE6(settings.getInt("longitudeE6", prefCurrentLocation.getLongitudeE6()));

            mapController.setZoom(prefZoomLevel);
            mapController.setCenter(prefCurrentLocation);
        }

        setupPOIalert();
    }

    private void initOsmdroid() {

        File osmdroid_path = OpenStreetMapTileProviderConstants.OSMDROID_PATH;

        Log.d ("osmdroid path", osmdroid_path.toString());

        sdcard = Environment.getExternalStorageDirectory();

        File dirs = new File(sdcard, tourDir);

        initHelp();

        if (dirs.mkdirs()) showHelp();

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 8000, 0, this);

        setContentView(R.layout.main);

        mapView = (MapView) findViewById(R.id.mapview);

        // Change offline-Tilesource directory name when choosing different source than Mapnik
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        mapView.setUseDataConnection(true);

        mapController = mapView.getController();

        locationOverlay = new SimpleLocationOverlay(this);
        mapView.getOverlays().add(locationOverlay);

        mapView.getOverlays().add(new ScaleBarOverlay(this));
        mapView.getOverlays().add(new MapTouchOverlay(this));
    }

    public class MapTouchOverlay extends Overlay {

        private Long lastTouchTime = (long) 0;

        public MapTouchOverlay(Context ctx) {super(ctx);}

        @Override
        protected void draw(Canvas c, MapView osmv, boolean shadow) { }

        @Override
        public boolean onTouchEvent(MotionEvent event, MapView mapView) {

            if (prefShowMetrics) {

                int actionType = event.getAction();

                switch (actionType) {

                    case (MotionEvent.ACTION_DOWN) :

                        lastTouchTime = System.currentTimeMillis();

                        break;

                    case (MotionEvent.ACTION_UP) :

                        long now = System.currentTimeMillis();

                        if (now - lastTouchTime > 2000) {

                            MapView.Projection proj = mapView.getProjection();

                            GeoPoint loc = (GeoPoint) proj.fromPixels((int)event.getX(), (int)event.getY());

                            double lat = (double)loc.getLatitudeE6() /1000000;
                            double lon = (double)loc.getLongitudeE6()/1000000;

                            Geopoint clickPoint = new Geopoint(lat, lon);

                            int i = track_kml.getClosestPoint(clickPoint);

                            Geopoint trackPoint = new Geopoint(track_kml.get(i).lat, track_kml.get(i).lon);

                            double distToTrack = (TrackPoint.getDistance(clickPoint, trackPoint));

                            String distToTrack_f;

                            if (distToTrack < 1) {

                                distToTrack *= 1000;

                                distToTrack_f = String.format("%1.0f", distToTrack) + " m";

                            } else {

                                distToTrack_f = String.format("%1.1f", distToTrack) + " km";
                            }

                            float trackDist = track_kml.getTrackLength(track_kml.get(0), trackPoint);

                            String trackDist_f = String.format("%1.1f", trackDist) + " km";

                            String tracklength_f = String.format("%1.1f", track_kml.trackLength);

                            float percentage = trackDist / track_kml.trackLength * 100;

                            String percentage_f = String.format("%1.0f", percentage) + " %";

                            twoPressAlert.setTitle(getString(R.string.alertGeoInfoTitle));

                            twoPressAlert.setMessage(Html.fromHtml(

                                getString(R.string.lat) + " : "  + Double.toString((lat))   + "°<br>" +
                                getString(R.string.lon) + " : "  + Double.toString((lon))   + "°<br>" +
                                getString(R.string.trackLength)       + " : "     + tracklength_f + "<br>" +
                                getString(R.string.dist_to_track)     + " : "     + distToTrack_f + "<br>" +
                                getString(R.string.dist_within_track) + " :<br>"  +
                                trackDist_f + " (" + percentage_f + ")"
                            ));

                            twoPressAlert.show();
                        }

                        lastTouchTime = (long) 0;

                        break;
                }
            }

            return false;
        }
    }

    private void setupPOIalert() {

        singleTapAlert = new AlertDialog.Builder(this);

        singleTapAlert.setNeutralButton(getString(R.string.alert_back), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                // just leave the menu
            }
        });

        singleTapAlert.setNegativeButton(getString(R.string.alert_forward), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                Intent viewDoc = new Intent(Intent.ACTION_VIEW);

                File file = new File(sdcard, tourDir + prefTourName + "/" + poiMapping + ".html");

                viewDoc.setDataAndType(Uri.fromFile(file), "text/html");

                PackageManager pm = getPackageManager();

                List<ResolveInfo> apps =
                        pm.queryIntentActivities(viewDoc, PackageManager.MATCH_DEFAULT_ONLY);

                if (apps.size() > 0) {

                    startActivity(viewDoc);

                } else {

                    Toast.makeText(JujuMap.this,
                            getString(R.string.toast_no_browser),
                            Toast.LENGTH_LONG).show();
                }

                /*
                WebView myWebView = (WebView) findViewById(R.id.webview);

                myWebView.setWebViewClient(new WebViewClient());

                WebSettings webSettings = myWebView.getSettings();
                webSettings.setJavaScriptEnabled(true);

                myWebView.loadUrl("http://www.spiegel.de");
                */

                /*AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
                builder1.setMessage("Write your message here.");
                builder1.setCancelable(true);
                builder1.setPositiveButton("Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                builder1.setNegativeButton("No",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog alert11 = builder1.create();
                alert11.show();*/
            }
        });

        twoPressAlert = new AlertDialog.Builder(this);

        twoPressAlert.setNeutralButton(getString(R.string.alert_back), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                // just leave the menu
            }
        });

        proximityAlert = new AlertDialog.Builder(this);

        proximityAlert.setNeutralButton(getString(R.string.alert_back), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                // just leave the menu
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {

        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent pData) {

        if ( requestCode == 1234 ) {

            if (resultCode == Activity.RESULT_OK ) {

                // benjamin_de_v0020

                prefTourName = pData.getExtras().getString( "newPath" );

                prefCountryCode = prefTourName.substring(prefTourName.length()-8, prefTourName.length()-6);

                initTour(prefTourName, prefCountryCode);
            }
        }
    }

    private void initTour(String prefTourName, String prefCountryCode) {

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("prefTourName"   , prefTourName);
        editor.putString("prefCountryCode", prefCountryCode);
        editor.commit();

        mapView.getOverlays().remove(track_kml_Overlay);
        mapView.getOverlays().remove(poi_kml_Overlay);

        track_kml = new Track();
        pois_kml  = new POIs();

        loadKML();

        prefCurrentLocation = track_kml.get_bBox().getCenter();

        mapView.getOverlays().add(track_kml_Overlay);

        if (prefShowPois) mapView.getOverlays().add(poi_kml_Overlay);

        mapView.zoomToBoundingBox(tour_bBox.increaseByScale((float) 1.2));

        Toast.makeText(this, getString(R.string.toast_new_tour) + prefTourName, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {

        switch (item.getItemId()) {

            case R.id.select_tour:

                Intent intent = new Intent (this, TourListOffline.class);

                intent.putExtra("osmpath", new File(sdcard, osmDir).toString());
                intent.putExtra("path", new File(sdcard, tourDir).toString());
                intent.putExtra("url" , prefDownloadUrl);

                startActivityForResult (intent, 1234);

                return true;

            case R.id.auto_zoom:

                autoZoom ^= true;

                return true;

            case R.id.show_tour:

                mapView.zoomToBoundingBox(tour_bBox.increaseByScale((float) 1.2));

                return true;

            case R.id.download_tour:

                Intent intent2 = new Intent (this, TourListOnline.class);

                intent2.putExtra("osmpath", new File(sdcard, osmDir).toString());
                intent2.putExtra("path", new File(sdcard, tourDir).toString());
                intent2.putExtra("url" , prefDownloadUrl);

                startActivityForResult (intent2, 1234);

                return true;

            case R.id.help:

                showHelp();

                return true;

            case R.id.options:

                startActivity(new Intent(this, Settings.class));

                return true;

            default:

                return super.onOptionsItemSelected(item);
        }
    }

    private void showHelp() {

        Intent viewDoc = new Intent(Intent.ACTION_VIEW);

        File file = new File(sdcard, tourDir + "/help.html");

        Uri uri = Uri.fromFile(file);

        viewDoc.setDataAndType(uri, "text/html");

        PackageManager pm = getPackageManager();

        List<ResolveInfo> apps = pm.queryIntentActivities(viewDoc, PackageManager.MATCH_DEFAULT_ONLY);

        if (apps.size() > 0) startActivity(viewDoc);
    }

    private void initHelp() {

        File file = new File(sdcard, tourDir + "/help.html");

        InputStream inputStream;

        OutputStream outputStream;

        try {

            inputStream = getResources().openRawResource(R.raw.help);

            outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[inputStream.available()];

            inputStream.read(buffer);

            outputStream.write(buffer);

            outputStream.close();
            inputStream.close();

        } catch(Exception ignored) {}
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {

        menu.findItem(R.id.select_tour).setEnabled(true);
        menu.findItem(R.id.select_tour).setTitle(R.string.select_tour);

        if (autoZoom)  menu.findItem(R.id.auto_zoom).setTitle(R.string.zoom_checked);
        else           menu.findItem(R.id.auto_zoom).setTitle(R.string.zoom_unchecked);

        menu.findItem(R.id.show_tour).setEnabled(track_kml.size() != 0);
        menu.findItem(R.id.show_tour).setTitle(R.string.show_tour);

        menu.findItem(R.id.download_tour).setEnabled(true);
        menu.findItem(R.id.download_tour).setTitle(R.string.download_tour);

        menu.findItem(R.id.help).setEnabled(true);
        menu.findItem(R.id.help).setTitle(R.string.help);

        menu.findItem(R.id.options).setEnabled(true);
        menu.findItem(R.id.options).setTitle(R.string.options);

        return super.onPrepareOptionsMenu(menu);
    }

    private void loadKML() {

        File file = new File(sdcard, tourDir + prefTourName + "/" + trackfile);

        if (file.exists()) {

            try {

                DefaultHandler handler = new KML_Parser(track_kml, pois_kml);

                SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

                saxParser.parse(file, handler);

            } catch (ParserConfigurationException e) {
                Log.e ("ParserConfigurationEx", "poitrack.kml could not be read.");
            } catch (SAXException e) {
                Log.e ("SAXException", "poitrack.kml could not be read.");
            } catch (IOException e) {
                Log.e ("IOException", "poitrack.kml could not be read.");
            }
        }

        track_kml_Overlay = new PathOverlay(Color.MAGENTA, this);

        for (TrackPoint tp : track_kml) {

            track_kml_Overlay.addPoint(new GeoPoint(tp.lat, tp.lon));
        }

        ArrayList <OverlayItem> anotherOverlayItemArray = new ArrayList <OverlayItem> ();

        int i = 0;

        for (PlacePoint pp : pois_kml) {

            anotherOverlayItemArray.add ( new OverlayItem (
                    String.format("%03d", ++i),
                    pp.name,
                    "<p>" + pp.description + "</p>",
                    new GeoPoint(pp.lat, pp.lon)
            ));
        }

        if (track_kml.get_bBox().getDiagonalLengthInMeters() > pois_kml.get_bBox().getDiagonalLengthInMeters()) {

            tour_bBox = track_kml.get_bBox();

        } else {

            tour_bBox = pois_kml.get_bBox();
        }

        poi_kml_Overlay = new ItemizedIconOverlay <OverlayItem> (
                this, anotherOverlayItemArray, onItemGestureListener);
    }

    public ItemizedIconOverlay.OnItemGestureListener <OverlayItem> onItemGestureListener
        = new ItemizedIconOverlay.OnItemGestureListener <OverlayItem>(){

        @Override
        public boolean onItemLongPress(int index, OverlayItem item) {

            return true;
        }

        @Override
        public boolean onItemSingleTapUp(int index, OverlayItem item) {

            singleTapAlert.setMessage(Html.fromHtml(
                    "<h2>" + item.getTitle() + "</h2><br>" +
                             item.getSnippet()
            ));

            poiMapping = item.getUid() + prefCountryCode;

            singleTapAlert.show();

            return true;
        }
    };

    public void onLocationChanged(Location location) {

        prefCurrentLocation = new GeoPoint(location);

        if (autoZoom) {

            mapController.setCenter(prefCurrentLocation);

            mapController.setZoom(prefAutoZoomLevel);
        }

        locationOverlay.setLocation(prefCurrentLocation);

        mapView.postInvalidate();

        double lat = (double)prefCurrentLocation.getLatitudeE6() /1000000;
        double lon = (double)prefCurrentLocation.getLongitudeE6()/1000000;

        Geopoint currentPoint = new Geopoint(lat, lon);

        int closePOI = pois_kml.getClosestPoint(currentPoint, prefAlarmDist);

        if (closePOI != -1) {

            try {

                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);

                r.play();

            } catch (Exception ignored) {}

            proximityAlert.setTitle(getString(R.string.proximity_alarm));
            proximityAlert.setMessage(Html.fromHtml(

                    getString(R.string.proximity_alarm_text) + " " +
                            pois_kml.get(closePOI).name +
                            "!"
            ));

            proximityAlert.show();
        }
    }

    public void onProviderDisabled(String provider) {}

    public void onProviderEnabled(String provider) {}

    public void onStatusChanged(String provider, int status, Bundle extras) {

        switch (status) {

            case LocationProvider.OUT_OF_SERVICE:

                Log.d ("onStatusChanged", "LocationProvider: " + provider + " OUT_OF_SERVICE");

                return;

            case LocationProvider.TEMPORARILY_UNAVAILABLE:

                Log.d ("onStatusChanged", "LocationProvider " + provider + " TEMPORARILY_UNAVAILABLE");

                return;

            case LocationProvider.AVAILABLE:

                Log.d ("onStatusChanged", "LocationProvider " + provider + " UNAVAILABLE");

                return;

            default:

                Log.d ("onStatusChanged", "LocationProvider " + provider + " unknown status: " + status);

        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        Log.d("PreferenceChanged", key);

        if (key.equals("prefShowPois")) {

            prefShowPois = settings.getBoolean("prefShowPois", prefShowPois);

            if (prefShowPois) mapView.getOverlays().add(poi_kml_Overlay);

            else mapView.getOverlays().remove(poi_kml_Overlay);

            mapView.postInvalidate();

        } else if (key.equals("prefShowMetrics")) {

            prefShowMetrics = settings.getBoolean("prefShowMetrics", prefShowMetrics);

        } else if (key.equals("prefShowAlarm")) {

            prefShowAlarm = settings.getBoolean("prefShowAlarm", prefShowAlarm);

        } else if (key.equals("prefLocale")) {

            prefLocale = settings.getString("prefLocale", prefLocale);

            if (! prefLocale.equals("default")) {

                Locale locale2 = new Locale(prefLocale);
                Locale.setDefault(locale2);
                Configuration config2 = new Configuration();
                config2.locale = locale2;
                getBaseContext().getResources().updateConfiguration(config2, getBaseContext().getResources().getDisplayMetrics());
            }
        }
    }

    @Override
    protected void onDestroy() {

        int zoomLevel   = mapView.getZoomLevel();

        GeoPoint center = mapView.getBoundingBox().getCenter();

        int latitudeE6  = center.getLatitudeE6();
        int longitudeE6 = center.getLongitudeE6();

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("prefZoomLevel", zoomLevel  );
        editor.putInt("latitudeE6"   , latitudeE6 );
        editor.putInt("longitudeE6"  , longitudeE6);
        editor.commit();

        super.onDestroy();
    }
}
