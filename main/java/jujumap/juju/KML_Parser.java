package jujumap.juju;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class KML_Parser extends DefaultHandler {

    private StringBuffer textBuffer  = null;
    private String       content     = null;
    private String       name        = null;
    private String       description = null;

    private int          folderDepth = 0;
    private int          nameCount   = 0;

    private Track gps_track;
    private POIs  roadbook;

    public KML_Parser (Track gps_track, POIs roadbook) {

        this.gps_track = gps_track;
        this.roadbook  = roadbook;
    }

    @Override
    public void startElement (String     namespaceURI,
                              String     localName,
                              String     qName,
                              Attributes attrs) throws SAXException {

        String elementName = ("".equals (localName)) ? qName : localName;

        if (elementName.equals ("Folder")) folderDepth = 1;

        if (elementName.equals ("name")) nameCount ++;
    }

    @Override
    public void endElement (String namespaceURI,
                            String localName,
                            String qName) throws SAXException {

        if (textBuffer != null) content = textBuffer.toString ().trim ();

        textBuffer = null;

        String elementName = ("".equals (localName)) ? qName : localName;

        if ( (elementName.equals ("coordinates")) && (folderDepth == 0)) {

            gps_track.addPath (content);
        }

        if ( (elementName.equals ("name")) && (nameCount > 3)) {

            name = content;
        }

        if ( (elementName.equals ("description")) && (folderDepth == 1)) {

            description = content;
        }

        if ( (elementName.equals ("coordinates")) && (folderDepth == 1)) {

            String coordinates = content;

            roadbook.addPlacePoint (name, coordinates, description);
        }
    }

    @Override
    public void characters (char[] buf, int offset, int len) throws SAXException {

        String s = new String (buf, offset, len);

        if (textBuffer == null) textBuffer = new StringBuffer (s);
        else                    textBuffer.append (s);
    }
}
