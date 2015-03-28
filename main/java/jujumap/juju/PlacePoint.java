package jujumap.juju;

public class PlacePoint extends Geopoint {

    String name        = "";
    String description = "";

    public PlacePoint (String name, String rawTrackPoint, String description) {

        super(rawTrackPoint);

        this.name = name;

        if (description.length() > 7) {

            this.description = description.substring(4, description.length()-3).trim();
        }
    }
}
