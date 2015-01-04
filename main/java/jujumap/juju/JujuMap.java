package jujumap.juju;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.app.Activity;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBoxE6;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.*;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.ArrayList;

public class JujuMap extends Activity implements LocationListener {

    MapView        mapView;
	IMapController mapController;

    GeoPoint currentLocation = new GeoPoint(49.598,11.005);

    Track track_kml = new Track();
    POIs  pois_kml  = new POIs();

    Track track_new = new Track();
    //POIs pois_new = new POIs();

    SimpleLocationOverlay locationOverlay;     // holds GPS-location

    PathOverlay          track_kml_Overlay;    // holds track
    ItemizedIconOverlay  poi_kml_Overlay;      // holds POIs

    PathOverlay track_new_Overlay;

    Boolean showPois  = false;
    Boolean showTrack = true;
    Boolean autoZoom  = true;

    AlertDialog.Builder alert;

    static final String TAG = JujuMap.class.getName();

    private String trackfile = "poitrack.kml";

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        loadKML();

        currentLocation = track_kml.get_bBox().getCenter();

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 8000, 0, this);

        setContentView(R.layout.main);

        mapView = (MapView) this.findViewById(R.id.mapview);

        // Change offline-Tilesource directory name when choosing different source than Mapnik
        mapView.setTileSource(TileSourceFactory.MAPNIK);

        mapView.getOverlays().add(track_kml_Overlay);

        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        mapView.setUseDataConnection(true);

	    mapController = mapView.getController();

        mapController.setZoom(13);
        mapController.setCenter(currentLocation);

        locationOverlay = new SimpleLocationOverlay(this);
	    mapView.getOverlays().add(locationOverlay);

        track_new_Overlay = new PathOverlay(Color.YELLOW, this);
        mapView.getOverlays().add(track_new_Overlay);

        mapView.getOverlays().add(new ScaleBarOverlay(this));

        alert = new AlertDialog.Builder(this);

        alert.setPositiveButton("Prev", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        alert.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        alert.setNegativeButton("Next", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu (Menu menu) {

        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    private void writeToSDFile(String s) {

        File sdcard = android.os.Environment.getExternalStorageDirectory();

        String fName = track_new.minTime + " - " + track_new.maxTime + ".txt";

        File file = new File(sdcard,"/osmdroid/" + fName);

        try {

            FileOutputStream f = new FileOutputStream(file);

            PrintWriter pw = new PrintWriter(f);

            pw.println(track_new.toString());

            pw.flush();
            pw.close();
            f.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {

        switch (item.getItemId()) {

            case R.id.load_kml:

                loadKML();

                if (showTrack) {

                    mapView.getOverlays().add(track_kml_Overlay);

                    mapView.postInvalidate();

                    if (autoZoom) mapView.zoomToBoundingBox(track_kml.get_bBox());
                }

                return true;

            case R.id.auto_zoom:

                autoZoom ^= true;

                return true;

            case R.id.show_track:

                showTrack ^= true;

                if (showTrack) mapView.getOverlays().add(track_kml_Overlay);
                else mapView.getOverlays().remove(track_kml_Overlay);

                mapView.postInvalidate();

                if (autoZoom) mapView.zoomToBoundingBox(track_kml.get_bBox());

                return true;

            case R.id.show_pois:

                showPois ^= true;

                if (showPois) mapView.getOverlays().add(poi_kml_Overlay);
                else mapView.getOverlays().remove(poi_kml_Overlay);

                mapView.postInvalidate();

                if (autoZoom) mapView.zoomToBoundingBox(pois_kml.get_bBox());

                return true;

            case R.id.save_kml:

                writeToSDFile(track_new.toString());

                return true;

            case R.id.del_kml:

                track_new.clear();

                track_new_Overlay.clearPath();

                mapView.postInvalidate();

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
            menu.findItem(R.id.load_kml).setEnabled(false);
        }

        if (track_new.size() == 0) menu.findItem(R.id.save_kml).setEnabled(false);
        else {
            menu.findItem(R.id.save_kml).setEnabled(true);
        }

        if (track_new.size() == 0) menu.findItem(R.id.del_kml).setEnabled(false);
        else {
            menu.findItem(R.id.del_kml).setEnabled(true);
        }

        if (pois_kml.size() == 0) menu.findItem(R.id.show_pois).setEnabled(false);
        else {
            menu.findItem(R.id.show_pois).setEnabled(true);
            menu.findItem(R.id.load_kml).setEnabled(false);
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

        File sdcard = Environment.getExternalStorageDirectory();

        File file = new File(sdcard, "/osmdroid/" + trackfile);

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

        for (PlacePoint pp : pois_kml) {

            anotherOverlayItemArray.add ( new OverlayItem (
                    pp.name,
                    "<p>" + pp.ortsInfo + "</p><p>" +
                    pp.geo + "</p><p>" + pp.roadInfo + "</p><p>" + pp.km + "</p>",
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

            alert.setMessage(Html.fromHtml("<h2>" + item.getTitle() + "</h2><br>" + item.getSnippet()));

            alert.show();

            return true;
        }

        @Override
        public boolean onItemSingleTapUp(int index, OverlayItem item) {

            Toast.makeText(JujuMap.this,
                    item.getTitle(),
                    Toast.LENGTH_LONG).show();

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

        track_new.addTP(
                location.getLatitude(),
                location.getLongitude(),
                location.getAltitude(),
                location.getAccuracy(),
                location.getSpeed(),
                location.getBearing(),
                location.getTime());

        track_new_Overlay.addPoint(currentLocation);

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
