package net.videgro.ships.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import net.videgro.ships.Analytics;
import net.videgro.ships.R;
import net.videgro.ships.fragments.SettingsFragment;

public class SettingsActivity extends Activity {
	private static final String TAG="SettingsActivity";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }
    
	public void onRequestRateClicked(View view){
		Analytics.logEvent(this,TAG, "onRequestRateClicked", "gotoGooglePlay");
		view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(view.getContext().getString(R.string.app_market_url))))	;
	}
}