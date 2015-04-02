package jujumap.juju;

import android.util.Log;
import org.osmdroid.util.BoundingBoxE6;

import java.util.ArrayList;
import java.util.Arrays;

public class POIs extends ArrayList <PlacePoint> {

    BoundingBoxE6 bBox;

    float minLon = +180;
    float maxLon = -180;
    float minLat =  +90;
    float maxLat =  -90;

    public POIs() { }

    public void addPlacePoint (String name, String rawCoords, String description) {

        PlacePoint pp = new PlacePoint (name, rawCoords, description);

        if (pp.lon < minLon) minLon = pp.lon;
        if (pp.lon > maxLon) maxLon = pp.lon;
        if (pp.lat < minLat) minLat = pp.lat;
        if (pp.lat > maxLat) maxLat = pp.lat;

        this.add (pp);
    }

    public int getClosestPoint(Geopoint geopoint, double alarmDist) {

        double minDist = 10000;
        int    min     =    -1;

        for (int i=0; i<size(); i++) {

            double dist = TrackPoint.getDistance(geopoint, get(i));

            get(i).distToCurrentLoc = dist;

            if (dist < minDist) {

                minDist = dist;
                min     = i;
            }
        }

        minDist = minDist * 1000;

        if (minDist <= alarmDist) {

            if (! get(min).proximity_alert) {

                for (PlacePoint placePoint : this) placePoint.proximity_alert = false;

                get(min).proximity_alert = true;

                return min;
            }

        } else {

            for (PlacePoint placePoint : this) {

                if (placePoint.distToCurrentLoc > alarmDist * 1.6) // hysteresis

                    placePoint.proximity_alert = false;
            }
        }

        return -1;
    }

    BoundingBoxE6 get_bBox () {

        bBox = new BoundingBoxE6 (maxLat, maxLon, minLat, minLon);

        return bBox;
    }
}

