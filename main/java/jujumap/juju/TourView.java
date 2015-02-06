package jujumap.juju;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TourView extends ListActivity {

    String path;

    @Override
    public void onCreate(Bundle bundle) {

        super.onCreate(bundle);

        Bundle b = getIntent().getExtras();

        path = b.getString("path");

        setContentView(R.layout.tour_view);

        ListAdapter adapter = createAdapter();

        setListAdapter(adapter);
    }

    protected ListAdapter createAdapter() {

        List valueList = new ArrayList <String> ();

        File[] files = new File(path).listFiles();

        for ( File aFile : files ) {

            if ( aFile.isDirectory() ) {

                valueList.add(aFile.getName());
            }
        }

        // Create a simple array adapter (of type string) with the test values
        ListAdapter adapter =
                new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1,
                        valueList);

        return adapter;
    }
}