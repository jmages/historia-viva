package jujumap.jjmap;

import org.osmdroid.util.BoundingBoxE6;
import java.util.ArrayList;

public class GeoList extends ArrayList <Geopoint> {

    BoundingBoxE6 bBox;

    float minLon = +180;
    float maxLon = -180;
    float minLat =  +90;
    float maxLat =  -90;

    public GeoList() { }

    BoundingBoxE6 get_bBox () {

        bBox = new BoundingBoxE6 (maxLat, maxLon, minLat, minLon);

        return bBox;
    }
}

