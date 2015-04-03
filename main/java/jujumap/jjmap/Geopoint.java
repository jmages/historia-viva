package jujumap.jjmap;

import java.util.ArrayList;
import java.util.Arrays;

public class Geopoint {

    float lon;
    float lat;
    float alt;

    float  accuracy;
    double speed;
    double bearing;

    long time;

    public Geopoint (String rawTrackPoint) {

        splitRawTrackPoint (rawTrackPoint);
    }

    public Geopoint(double lat, double lon) {

        this.lat = (float) lat;
        this.lon = (float) lon;
    }

    private void splitRawTrackPoint (String rawTrackPoint) {

        ArrayList <String> rawGeoData = new ArrayList <String> (Arrays.asList (rawTrackPoint.split (",")));

        switch (rawGeoData.size ()) {

            case 2:

                lon = Float.valueOf (rawGeoData.get(0));
                lat = Float.valueOf (rawGeoData.get(1));
                alt = 0;

                break;

            case 3:

                lon = Float.valueOf (rawGeoData.get(0));
                lat = Float.valueOf (rawGeoData.get(1));
                alt = Float.valueOf (rawGeoData.get(2));

                break;

            default:
                System.out.println ("Error: Trackpoint data is not valid: " + rawTrackPoint);
                System.exit (1);
        }
    }
}
