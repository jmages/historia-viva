package jujumap.juju;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Settings extends PreferenceActivity {

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        addPreferencesFromResource(R.xml.preferences);
    }
}