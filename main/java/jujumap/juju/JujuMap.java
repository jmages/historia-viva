package jujumap.juju;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
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

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class JujuMap extends Activity implements LocationListener {

    String trackName   = "benjamin_de";
    String trackfile   = "poitrack.kml";
    String mainDir     = "/osmdroid/historia-viva/";
    String downloadUrl = "http://www.historia-viva.net/downloads/";

    File   sdcard;

    Boolean showPois  = false;
    Boolean showTrack = false;
    Boolean autoZoom  = true;

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

    static final String TAG = JujuMap.class.getName();

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        File osmdroid_path = OpenStreetMapTileProviderConstants.OSMDROID_PATH;

        Log.d ("osmdroid path", osmdroid_path.toString());

        sdcard = Environment.getExternalStorageDirectory();

        loadKML();

        currentLocation = track_kml.get_bBox().getCenter();

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 8000, 0, this);

        setContentView(R.layout.main);

        mapView = (MapView) findViewById(R.id.mapview);

        // Change offline-Tilesource directory name when choosing different source than Mapnik
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        mapView.getOverlays().add(track_kml_Overlay);
        mapView.getOverlays().add(poi_kml_Overlay);

        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        mapView.setUseDataConnection(true);

	    mapController = mapView.getController();

        mapController.setZoom(12);
        mapController.setCenter(currentLocation);

        locationOverlay = new SimpleLocationOverlay(this);
	    mapView.getOverlays().add(locationOverlay);

        mapView.getOverlays().add(new ScaleBarOverlay(this));

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

                File file = new File(sdcard, mainDir + trackName + "/" + poiMapping + ".html");

                viewDoc.setDataAndType(Uri.fromFile(file), "text/html");

                PackageManager pm = getPackageManager();

                List <ResolveInfo> apps =
                        pm.queryIntentActivities(viewDoc, PackageManager.MATCH_DEFAULT_ONLY);

                if (apps.size() > 0) {

                    startActivity(viewDoc);

                } else {

                    Toast.makeText(JujuMap.this,
                            "No HTML-Browser found!",
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
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent pData) {

        if ( requestCode == 1234 ) {

            if (resultCode == Activity.RESULT_OK ) {

                final String zData = pData.getExtras().getString( "newPath" );

                trackName = zData;

                mapView.getOverlays().remove(track_kml_Overlay);
                mapView.getOverlays().remove(poi_kml_Overlay);

                track_kml = new Track();
                pois_kml  = new POIs();

                loadKML();

                mapView.getOverlays().add(track_kml_Overlay);
                mapView.getOverlays().add(poi_kml_Overlay);

                currentLocation = track_kml.get_bBox().getCenter();

                mapView.zoomToBoundingBox(track_kml.get_bBox());
                //mapController.setZoom(12);
                mapController.setCenter(currentLocation);

                Toast.makeText(this, "New Tour: " + zData, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {

        switch (item.getItemId()) {

            case R.id.load_kml:

                Intent intent = new Intent (this, TourView.class);

                intent.putExtra("path", new File(sdcard, mainDir).toString());
                intent.putExtra("url" , downloadUrl);

                startActivityForResult (intent, 1234);

                /*
                loadKML();

                if (showTrack) {

                    mapView.getOverlays().add(track_kml_Overlay);

                    mapView.postInvalidate();

                    if (autoZoom) mapView.zoomToBoundingBox(track_kml.get_bBox());
                }*/

                return true;

            case R.id.auto_zoom:

                autoZoom ^= true;

                return true;

            case R.id.show_track:

                mapView.zoomToBoundingBox(track_kml.get_bBox());

                return true;

            case R.id.show_pois:

                mapView.zoomToBoundingBox(pois_kml.get_bBox());

                return true;

            case R.id.save_kml:

                return true;

            case R.id.del_kml:

                return true;

            default:

                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {

        if (track_kml.size() == 0) menu.findItem(R.id.show_track).setEnabled(false);
        else {
            menu.findItem(R.id.show_track).setEnabled(true);
            menu.findItem(R.id.load_kml).setEnabled(true);
        }

        if (true) menu.findItem(R.id.save_kml).setEnabled(false);
        else {
            menu.findItem(R.id.save_kml).setEnabled(true);
        }

        if (true) menu.findItem(R.id.del_kml).setEnabled(false);
        else {
            menu.findItem(R.id.del_kml).setEnabled(true);
        }

        if (pois_kml.size() == 0) menu.findItem(R.id.show_pois).setEnabled(false);
        else {
            menu.findItem(R.id.show_pois).setEnabled(true);
            menu.findItem(R.id.load_kml).setEnabled(true);
        }

        if (autoZoom)  menu.findItem(R.id.auto_zoom).setTitle(R.string.zoom_checked);
        else           menu.findItem(R.id.auto_zoom).setTitle(R.string.zoom_unchecked);

        if (showTrack) menu.findItem(R.id.show_track).setTitle(R.string.show_track_checked);
        else           menu.findItem(R.id.show_track).setTitle(R.string.show_track_unchecked);

        if (showPois)  menu.findItem(R.id.show_pois).setTitle(R.string.show_pois_checked);
        else           menu.findItem(R.id.show_pois).setTitle(R.string.show_pois_unchecked);

        return super.onPrepareOptionsMenu(menu);
    }

    private void loadKML() {

        File file = new File(sdcard, mainDir + trackName + "/" + trackfile);

        try {

            DefaultHandler handler = new KML_Parser(track_kml, pois_kml);

            SAXParser saxParser = SAXParserFactory.newInstance().newSAXParser();

            saxParser.parse(file, handler);

        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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

            poiMapping = item.getUid() + "de";

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

                Log.d (TAG, "LocationProvider: " + provider + " OUT_OF_SERVICE");

                return;

            case LocationProvider.TEMPORARILY_UNAVAILABLE:

                Log.d (TAG, "LocationProvider " + provider + " TEMPORARILY_UNAVAILABLE");

                return;

            case LocationProvider.AVAILABLE:

                Log.d (TAG, "LocationProvider " + provider + " UNAVAILABLE");

                return;

            default:

                Log.d (TAG, "LocationProvider " + provider + " unknown status: " + status);

        }
    }
}
