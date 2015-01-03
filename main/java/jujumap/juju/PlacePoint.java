package jujumap.juju;

import java.util.ArrayList;
import java.util.Arrays;

public class PlacePoint extends Geopoint {

    String name     = "";

    String url      = "";
    String ortsInfo = "";
    String geo      = "";
    String roadInfo = "";
    String km       = "";

    public PlacePoint (String name, String rawTrackPoint, String description) {

        super(rawTrackPoint);

        this.name = name;

        splitDescription (description);

    }

    private void splitDescription(String description) {

        ArrayList<String> rawInfo = new ArrayList <String> (Arrays.asList(description.split("<!--")));

        if (rawInfo.size() == 5) {

            url      = rawInfo.get(0);
            ortsInfo = rawInfo.get(1).substring(0, rawInfo.get(1).length()-8).trim();
            geo      = rawInfo.get(2).substring(0, rawInfo.get(2).length()-8).trim();
            roadInfo = rawInfo.get(3).substring(0, rawInfo.get(3).length()-8).trim();
            km       = rawInfo.get(4).substring(0, rawInfo.get(4).length()-3).trim();
        }
    }
}
