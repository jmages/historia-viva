package jujumap.juju;

import android.util.Log;
import org.osmdroid.util.BoundingBoxE6;

import java.util.ArrayList;
import java.util.Arrays;

public class Track extends ArrayList <TrackPoint> {

    float minLon = +180;
    float maxLon = -180;
    float minLat =  +90;
    float maxLat =  -90;

    float minAlt = 10000;
    float maxAlt = -1000;

    float trackLength = 0;

    public void addPath (String rawPath) {

        TrackPoint tp;

        ArrayList <String> rawTrackpoints = new ArrayList <String> (Arrays.asList (rawPath.split (" ")));

        for (String rawTrackpoint : rawTrackpoints) {

            tp = new TrackPoint (rawTrackpoint);

            if (tp.lon < minLon) minLon = tp.lon;
            if (tp.lon > maxLon) maxLon = tp.lon;
            if (tp.lat < minLat) minLat = tp.lat;
            if (tp.lat > maxLat) maxLat = tp.lat;

            if (tp.alt < minAlt) minAlt = tp.alt;
            if (tp.alt > maxAlt) maxAlt = tp.alt;

            this.add (tp);
        }

        if (size() > 0) {

            trackLength = getTrackLength(get(0), get(size()-1));

            Log.d("addPath", String.valueOf(trackLength));
        }
    }

    public BoundingBoxE6 get_bBox () {

        return new BoundingBoxE6 (maxLat, maxLon, minLat, minLon);
    }
    public int getClosestPoint (Geopoint placeMark) {

        double lat = placeMark.lat;
        double lon = placeMark.lon;
        double dist = 1000;

        TrackPoint tp;
        TrackPoint tpNear = null;

        for (Object o : this) {
            tp = (TrackPoint) o;
            if (dist > Math.abs (lon - tp.lon) + Math.abs (lat - tp.lat)) {
                dist = Math.abs (lon - tp.lon) + Math.abs (lat - tp.lat);
                tpNear = tp;
            }
        }

        return indexOf (tpNear);

        /* more exact but slower
        if (dist > TrackPoint.getDistance(tp, new TrackPoint(lat, lon))) {
            dist = TrackPoint.getDistance(tp, new TrackPoint(lat, lon));
            tpNear = tp;*/

    }

    public float getTrackLength(TrackPoint tp0, TrackPoint tp1) {

        float dist = 0;

        int i0 = getClosestPoint (tp0);
        int i1 = getClosestPoint (tp1);

        for (int i = i0 + 1; i <= i1; i++) {
            dist += TrackPoint.getDistance (get (i - 1), get (i));
            //System.out.print(((TrackPoint)get(i)).rawTrackPoint);
            //System.out.println(" : " + dist);
        }

        return dist;
    }

    @Override
    public void clear() {

        super.clear();

        minLon = +180;
        maxLon = -180;
        minLat =  +90;
        maxLat =  -90;

        minAlt = 10000;
        maxAlt = -1000;

        trackLength = 0;
    }
}