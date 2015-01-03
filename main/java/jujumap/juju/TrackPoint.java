package jujumap.juju;

public class TrackPoint extends Geopoint {

    public TrackPoint (String rawTrackPoint) {

        super(rawTrackPoint);
    }

    public TrackPoint(double lat, double lon, double alt, float accuracy, double speed, double bearing, long time) {

        super(lat, lon, alt, accuracy, speed, bearing, time);
    }
}
