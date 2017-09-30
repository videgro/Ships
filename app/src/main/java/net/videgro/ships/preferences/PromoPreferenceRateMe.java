package net.videgro.ships.preferences;

import net.videgro.ships.R;
import android.content.Context;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class PromoPreferenceRateMe extends Preference {

	public PromoPreferenceRateMe(Context context) {
		super(context, null);
	}

	public PromoPreferenceRateMe(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		super.onCreateView(parent);
		final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		return inflater.inflate(R.layout.promo_rate_me, null);
	}
}