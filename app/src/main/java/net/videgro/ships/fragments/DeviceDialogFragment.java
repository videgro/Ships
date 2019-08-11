package net.videgro.ships.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import net.videgro.ships.R;
import net.videgro.usb.rtlsdr.RtlSdrDevice;

import java.util.List;
import java.util.Locale;

public class DeviceDialogFragment extends DialogFragment {
    private final static String RTL_SDR_DEVICE = "rtlSdrDevice_%d";
    private final static String RTL_SDR_DEVICES_COUNT = "rtlSdrDevices_count";

    private final static Object LOCK = new Object();

    public static DialogFragment invokeDialog(List<RtlSdrDevice> devices) {
        final Bundle b = new Bundle();

        synchronized (LOCK) {
            b.putInt(RTL_SDR_DEVICES_COUNT, devices.size());

            for (int id = 0; id < devices.size(); id++) {
                final RtlSdrDevice device = devices.get(id);
                if (device != null) {
                    b.putParcelable(String.format(Locale.getDefault(), RTL_SDR_DEVICE, id), device);
                }
            }
        }

        final DeviceDialogFragment dmg = new DeviceDialogFragment();
        dmg.setArguments(b);

        return dmg;
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog result=null;
        final DeviceDialogListener callback = (DeviceDialogListener) getActivity();
        final Bundle b = getArguments();
        if (b!=null) {
            final int devicesCount = b.getInt(RTL_SDR_DEVICES_COUNT);
            final RtlSdrDevice[] devices = new RtlSdrDevice[devicesCount];
            final String[] options = new String[devicesCount];
            for (int id = 0; id < devicesCount; id++) {
                final Parcelable parcelable = b.getParcelable(String.format(Locale.getDefault(), RTL_SDR_DEVICE, id));
                if (parcelable instanceof RtlSdrDevice) {
                    final RtlSdrDevice rtlSdrDevice = (RtlSdrDevice) parcelable;
                    devices[id] = rtlSdrDevice;
                    options[id] = rtlSdrDevice.getFriendlyName();
                }
            }

            result= new AlertDialog.Builder(getActivity())
                    .setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (callback != null) {
                                final RtlSdrDevice selected = devices[which];
                                callback.onDeviceDialogDeviceChosen(selected);
                            }
                        }
                    })
                    .setIcon(R.drawable.ic_stat_usb)
                    .setTitle(getString(R.string.popup_select_device_title))
                    .create();
        }
        return result;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (isAdded()) {
            final FragmentActivity fragmentActivity=getActivity();
            if (fragmentActivity instanceof DeviceDialogListener) {
                ((DeviceDialogListener) fragmentActivity).onDeviceDialogCanceled();
            }
        }
    }

    public interface DeviceDialogListener {
        void onDeviceDialogDeviceChosen(RtlSdrDevice selected);
        void onDeviceDialogCanceled();
    }
}
