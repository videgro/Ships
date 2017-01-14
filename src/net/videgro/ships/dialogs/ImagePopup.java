package net.videgro.ships.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.drawable.ColorDrawable;
import android.text.Html;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import net.videgro.ships.Analytics;
import net.videgro.ships.R;

public class ImagePopup extends Dialog implements OnDismissListener {
	final String TAG="ImagePopup";
	
	private static final int SIZE_PERCENTAGE=60;
	
	private int id;
	private ImagePopupListener listener;
	
	public ImagePopup(int id,Context context,ImagePopupListener listener,String text,int imageResource) {
		super(context);
		this.id=id;
		this.listener=listener;
		
		// Set costume dialog information
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setCanceledOnTouchOutside(true);
		setContentView(R.layout.image_popup);
		setOnDismissListener(this);
		
		// get current screen h/w
		Display d = ((Activity) context).getWindowManager().getDefaultDisplay();
		int w = d.getWidth();
		int h = d.getHeight();

		// Set popup window h/w full screen or 98% it up to you
		getWindow().setLayout((int) (w / 100) * SIZE_PERCENTAGE, (int) (h / 100) * SIZE_PERCENTAGE);
		getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
		setCancelable(true);

		// Image
		ImageView imageView = (ImageView) findViewById(R.id.imageView);
		try {
			imageView.setImageResource(imageResource);
		} catch (OutOfMemoryError e){
			/*
			 * FIXME: Observed exception: "OutOfMemoryError (@ImagePopup:<init>:46) {main}"
			 */
			final String msg="Drawing image - OutOfMemoryError";
			Log.e(TAG,msg+": "+e.getMessage());
			Analytics.logEvent(context, TAG,msg,e.getMessage());
		}
	
		// Text
		TextView textView = (TextView) findViewById(R.id.textView);
		textView.setText(Html.fromHtml(text));		
	}
		
	@Override
	  public boolean onTouchEvent(MotionEvent event) {
	    // Tap anywhere to close dialog.
	    dismiss();
	    return true;
	  }
	
    @Override
    public void onDismiss(DialogInterface dialog) {
    	listener.onImagePopupDispose(id);
    }
	
	public interface ImagePopupListener{
		void onImagePopupDispose(final int id);
	}
}
