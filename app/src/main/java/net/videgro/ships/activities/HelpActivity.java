package net.videgro.ships.activities;

import android.app.Activity;
import android.os.Bundle;

import net.videgro.ships.fragments.HelpFragment;

public class HelpActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, new HelpFragment()).commit();
    }
}