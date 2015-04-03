package jujumap.jjmap;

public class TrackPoint extends Geopoint {

    public TrackPoint (String rawTrackPoint) {

        super(rawTrackPoint);
    }

    static double getDistance (Geopoint tp1, Geopoint tp2) {

        double lat1 = tp1.lat / 180 * Math.PI;
        double lon1 = tp1.lon / 180 * Math.PI;
        double lat2 = tp2.lat / 180 * Math.PI;
        double lon2 = tp2.lon / 180 * Math.PI;

        double dist = Math.acos (
                Math.sin (lat1) * Math.sin (lat2) +
                Math.cos (lat1) * Math.cos (lat2) *
                Math.cos (lon2 - lon1)
        ) * 6378.137; // radius of the equator

        if (Double.isNaN (dist)) dist = 0;

        return dist;
    }
}
