package net.videgro.usb.rtlsdr;

import android.hardware.usb.UsbDevice;
import android.os.Parcel;
import android.os.Parcelable;

public class RtlSdrDevice implements Parcelable {
    private final UsbDevice usbDevice;
    private final String friendlyName;

    public RtlSdrDevice(final UsbDevice usbDevice,final String friendlyName){
        this.usbDevice=usbDevice;
        this.friendlyName=friendlyName;
    }
    public static final Parcelable.Creator<RtlSdrDevice> CREATOR =
            new Parcelable.Creator<RtlSdrDevice>() {
                public RtlSdrDevice createFromParcel(Parcel in) {
                    final Parcelable usbDevice=in.readParcelable(UsbDevice.class.getClassLoader());
                    if (usbDevice==null){
                        throw new IllegalArgumentException("parcelable is null");
                    }

                    final String friendlyName=in.readString();

                    return new RtlSdrDevice((UsbDevice)usbDevice,friendlyName);
                }

                public RtlSdrDevice[] newArray(int size) {
                    return new RtlSdrDevice[size];
                }
            };

    public String getFriendlyName() {
        return friendlyName;
    }

    public UsbDevice getUsbDevice(){
        return usbDevice;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(usbDevice,flags);
        dest.writeString(friendlyName);
    }
}
