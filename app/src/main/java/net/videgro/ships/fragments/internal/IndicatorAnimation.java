package net.videgro.ships.fragments.internal;

import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;

public class IndicatorAnimation extends AnimationSet {
    private final static int DURATION=1000;

    public IndicatorAnimation(boolean shareInterpolator) {
        super(shareInterpolator);

        final Animation fadeIn = new AlphaAnimation(0,1);
                        fadeIn.setInterpolator(new DecelerateInterpolator());
                        fadeIn.setDuration(DURATION);

        final Animation fadeOut = new AlphaAnimation(1,0);
                        fadeOut.setInterpolator(new AccelerateInterpolator());
                        fadeOut.setStartOffset(DURATION);
                        fadeOut.setDuration(DURATION);

        addAnimation(fadeIn);
        addAnimation(fadeOut);
        setFillAfter(true);
    }
}
