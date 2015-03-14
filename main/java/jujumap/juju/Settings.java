package jujumap.juju;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Settings extends PreferenceActivity {

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //getPreferenceManager().setSharedPreferencesName("MyPrefs");
        //getPreferenceManager().setSharedPreferencesMode(MODE_WORLD_WRITEABLE);
        //getSharedPreferences("preferences", MODE_PRIVATE);

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        addPreferencesFromResource(R.xml.preferences);

    }
}