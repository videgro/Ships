package net.videgro.ships.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import net.videgro.ships.Analytics;
import net.videgro.ships.R;
import net.videgro.ships.SettingsUtils;
import net.videgro.ships.fragments.ShowMapFragment;

public class MainActivity extends Activity {
			
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// FIXME: Observed exception "IllegalAccessException (@MainActivity:onCreate:16) {main}"

        // Init some singletons which need the Context
        Analytics.getInstance().init(this);
        SettingsUtils.getInstance().init(this);

		setContentView(R.layout.activity_main);
	
		final ActionBar actionBar=getActionBar();
		actionBar.setDisplayShowTitleEnabled(true);
		//actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		
		getFragmentManager().beginTransaction().replace(R.id.container, new ShowMapFragment()).commit();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean result = false;
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_help:
				openHelp();
				result = true;
				break;
			case R.id.action_settings:
				openSettings();
				result = true;
				break;
		default:
			result = super.onOptionsItemSelected(item);
		}
		return result;
	}

	private void openSettings() {
		startActivity(new Intent(this, SettingsActivity.class));
	}

    private void openHelp() {
        startActivity(new Intent(this, HelpActivity.class));
    }
}
