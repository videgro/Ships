package net.videgro.ships.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import net.videgro.ships.R;
import net.videgro.ships.Utils;

public class HelpFragment extends Fragment {
	private static final String TAG = "HelpFragment";

	private RelativeLayout adView;

	@SuppressLint("NewApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final View rootView = inflater.inflate(R.layout.fragment_help, container, false);

		final TextView textView = (TextView)rootView.findViewById(R.id.textViewHelp);
		textView.setText(Html.fromHtml(getString(R.string.text_help)));
		textView.setMovementMethod(LinkMovementMethod.getInstance());

		final RelativeLayout adView = rootView.findViewById(R.id.adView);

		if (isAdded()) {
			Utils.loadAd(getActivity(),adView,getString(R.string.adUnitId_HelpFragment));
		}

		return rootView;
	}
}
