package jujumap.juju;

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

    BoundingBoxE6 get_bBox () {

        bBox = new BoundingBoxE6 (maxLat, maxLon, minLat, minLon);

        return bBox;
    }
}

