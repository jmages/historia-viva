package jujumap.juju;

import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.List;

public class TourView extends ListActivity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {

        super.onCreate(icicle);

        setContentView(R.layout.tour_view);

        ListAdapter adapter = createAdapter();

        setListAdapter(adapter);
    }

    /**
     * Creates and returns a list adapter for the current list activity
     * @return
     */
    protected ListAdapter createAdapter()
    {
        // List with strings of contacts name
        //contactList = ... someMethod to get your list ...

        List valueList = new ArrayList <String>();

        for (int i = 0; i < 10; i++) {

            valueList.add("value");
        }

        // Create a simple array adapter (of type string) with the test values
        ListAdapter adapter =
                new ArrayAdapter<String>(this,
                        android.R.layout.simple_list_item_1,
                        valueList);

        return adapter;
    }
}