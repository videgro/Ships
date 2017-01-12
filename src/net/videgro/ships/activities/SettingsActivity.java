package net.videgro.ships.activities;

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;
import net.videgro.ships.R;
import net.videgro.ships.fragments.SettingsFragment;

public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }
}