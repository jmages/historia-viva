package jujumap.juju;

public class TrackPoint extends Geopoint {

    public TrackPoint (String rawTrackPoint) {

        super(rawTrackPoint);
    }

    public TrackPoint(double lat, double lon, double alt, float accuracy, double speed, double bearing, long time) {

        super(lat, lon, alt, accuracy, speed, bearing, time);
    }

    static double getDistance (TrackPoint tp1, TrackPoint tp2) {

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
