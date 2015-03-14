package jujumap.juju;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.constants.OpenStreetMapTileProviderConstants;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.SimpleLocationOverlay;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class JujuMap extends Activity implements LocationListener, SharedPreferences.OnSharedPreferenceChangeListener {

    String trackName       = "";
    String countryCode     = "";
    int zoomLevel          = 12;
    String trackfile       = "poitrack.kml";
    String osmDir          = "/osmdroid";
    String tourDir         = osmDir + "/historia-viva/";
    String downloadUrl     = "http://www.historia-viva.net/downloads/";
    Boolean showPois       = true;

    File   sdcard;

    Boolean autoZoom  = false;

    String   poiMapping      = "";
    GeoPoint currentLocation = new GeoPoint(49.598,11.005);

    MapView        mapView;
	IMapController mapController;

    Track track_kml = new Track();
    POIs  pois_kml  = new POIs();

    SimpleLocationOverlay locationOverlay;     // holds GPS-location

    PathOverlay          track_kml_Overlay;    // holds poitrack
    ItemizedIconOverlay  poi_kml_Overlay;      // holds POIs

    AlertDialog.Builder alert;

    SharedPreferences settings;
    SharedPreferences.Editor editor;

    @Override
    public void onCreate (Bundle savedInstanceState) {

        super.onCreate (savedInstanceState);

        settings = PreferenceManager.getDefaultSharedPreferences(this);

        settings.registerOnSharedPreferenceChangeListener(this);

        editor   = settings.edit();

        initOsmdroid();

        trackName   = settings.getString ("trackName"  , trackName  );
        countryCode = settings.getString ("countryCode", countryCode);
        showPois    = settings.getBoolean("showPois"   , showPois);

        if (trackName.equals("")) {

            Intent intent = new Intent (this, TourListOffline.class);

            intent.putExtra("osmpath", new File(sdcard, osmDir).toString());
            intent.putExtra("path"   , new File(sdcard, tourDir).toString());
            intent.putExtra("url"    , downloadUrl);

            startActivityForResult (intent, 1234);

        } else {

            loadKML();

            mapView.getOverlays().add(track_kml_Overlay);

            if (showPois) mapView.getOverlays().add(poi_kml_Overlay);

            zoomLevel = settings.getInt("zoomLevel"  , zoomLevel);

            currentLocation.setLatitudeE6 (settings.getInt("latitudeE6" , currentLocation.getLatitudeE6 ()));
            currentLocation.setLongitudeE6(settings.getInt("longitudeE6", currentLocation.getLongitudeE6()));

            mapController.setZoom(zoomLevel);
            mapController.setCenter(currentLocation);
        }

        setupAlert();
    }

    private void initOsmdroid() {

        File osmdroid_path = OpenStreetMapTileProviderConstants.OSMDROID_PATH;

        Log.d ("osmdroid path", osmdroid_path.toString());

        sdcard = Environment.getExternalStorageDirectory();

        File dirs = new File(sdcard, tourDir);

        if (! dirs.exists()) {

            dirs.mkdirs();
        }

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
    }

    private void setupAlert() {

        alert = new AlertDialog.Builder(this);

        alert.setNeutralButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                // just leave the menu
            }
        });

        alert.setNegativeButton("HTML", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                Intent viewDoc = new Intent(Intent.ACTION_VIEW);

                File file = new File(sdcard, tourDir + trackName + "/" + poiMapping + ".html");

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

                trackName = pData.getExtras().getString( "newPath" );

                countryCode = trackName.substring(trackName.length()-8,trackName.length()-6);

                editor.putString("trackName"  , trackName);
                editor.putString("countryCode", countryCode);
                editor.commit();

                initTour();
            }
        }
    }

    private void initTour() {

        mapView.getOverlays().remove(track_kml_Overlay);
        mapView.getOverlays().remove(poi_kml_Overlay);

        track_kml = new Track();
        pois_kml  = new POIs();

        loadKML();

        currentLocation = track_kml.get_bBox().getCenter();

        mapView.getOverlays().add(track_kml_Overlay);

        if (showPois) mapView.getOverlays().add(poi_kml_Overlay);

        mapView.zoomToBoundingBox(track_kml.get_bBox());
        //mapController.setZoom(12);
        mapController.setCenter(currentLocation);

        Toast.makeText(this, getString(R.string.toast_new_tour) + trackName, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {

        switch (item.getItemId()) {

            case R.id.select_tour:

                Intent intent = new Intent (this, TourListOffline.class);

                intent.putExtra("osmpath", new File(sdcard, osmDir).toString());
                intent.putExtra("path", new File(sdcard, tourDir).toString());
                intent.putExtra("url" , downloadUrl);

                startActivityForResult (intent, 1234);

                return true;

            case R.id.auto_zoom:

                autoZoom ^= true;

                return true;

            case R.id.show_tour:

                mapView.zoomToBoundingBox(track_kml.get_bBox());

                return true;

            case R.id.download_tour:

                Intent intent2 = new Intent (this, TourListOnline.class);

                intent2.putExtra("osmpath", new File(sdcard, osmDir).toString());
                intent2.putExtra("path", new File(sdcard, tourDir).toString());
                intent2.putExtra("url" , downloadUrl);

                startActivityForResult (intent2, 1234);

                return true;

            case R.id.unused:

                return true;

            case R.id.options:

                startActivity(new Intent(this, Settings.class));

                return true;

            default:

                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {

        menu.findItem(R.id.select_tour).setEnabled(true);

        if (autoZoom)  menu.findItem(R.id.auto_zoom).setTitle(R.string.zoom_checked);
        else           menu.findItem(R.id.auto_zoom).setTitle(R.string.zoom_unchecked);

        menu.findItem(R.id.show_tour).setEnabled(true);
        menu.findItem(R.id.download_tour).setEnabled(true);
        menu.findItem(R.id.unused).setEnabled(false);
        menu.findItem(R.id.options).setEnabled(true);

        return super.onPrepareOptionsMenu(menu);
    }

    private void loadKML() {

        File file = new File(sdcard, tourDir + trackName + "/" + trackfile);

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

            alert.setMessage(Html.fromHtml(
                    "<h2>" + item.getTitle() + "</h2><br>" +
                            item.getSnippet()
            ));

            poiMapping = item.getUid() + countryCode;

            alert.show();

            return true;
        }
    };

    public void onLocationChanged(Location location) {

        currentLocation = new GeoPoint(location);

        if (autoZoom) {

            mapController.setCenter(currentLocation);

            mapController.setZoom(16);
        }

        locationOverlay.setLocation(currentLocation);

        mapView.postInvalidate();
    }

    public void onProviderDisabled(String provider) {
    }

    public void onProviderEnabled(String provider) {
    }

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

        if (key.equals("showPois")) {

            showPois = settings.getBoolean("showPois", showPois);

            if (showPois) mapView.getOverlays().add(poi_kml_Overlay);

            else mapView.getOverlays().remove(poi_kml_Overlay);

            mapView.postInvalidate();
        }
    }

    @Override
    protected void onDestroy() {

        int zoomLevel   = mapView.getZoomLevel();

        GeoPoint center = mapView.getBoundingBox().getCenter();

        int latitudeE6  = center.getLatitudeE6();
        int longitudeE6 = center.getLongitudeE6();

        editor.putInt("zoomLevel", zoomLevel);
        editor.putInt("latitudeE6",  latitudeE6 );
        editor.putInt("longitudeE6", longitudeE6);

        editor.commit();

        super.onDestroy();
    }
}
