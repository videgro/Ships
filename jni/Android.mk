LOCAL_PATH:= $(call my-dir)
APP_PLATFORM:= android-16
APP_ABI:= armeabi armeabi-v7a x86
    
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
 libusb/libusb/core.c \
 libusb/libusb/descriptor.c \
 libusb/libusb/hotplug.c \
 libusb/libusb/io.c \
 libusb/libusb/sync.c \
 libusb/libusb/strerror.c \
 libusb/libusb/os/linux_usbfs.c \
 libusb/libusb/os/poll_posix.c \
 libusb/libusb/os/threads_posix.c \
 libusb/libusb/os/linux_netlink.c \
 rtl-sdr/src/rtl-sdr-android.c \
 rtl-sdr/src/convenience/convenience.c \
 rtl-sdr/src/tuner_e4k.c \
 rtl-sdr/src/tuner_fc0012.c \
 rtl-sdr/src/tuner_fc0013.c \
 rtl-sdr/src/tuner_fc2580.c \
 rtl-sdr/src/tuner_r82xx.c \
 rtl-sdr/src/rtl_test.c \
 rtl-ais/main.c \
 rtl-ais/rtl_ais.c \
 rtl-ais/aisdecoder/aisdecoder.c \
 rtl-ais/aisdecoder/sounddecoder.c \
 rtl-ais/aisdecoder/lib/receiver.c \
 rtl-ais/aisdecoder/lib/protodec.c \
 rtl-ais/aisdecoder/lib/hmalloc.c \
 rtl-ais/aisdecoder/lib/filter.c \
 rtl-ais/tcp_listener/tcp_listener.c \
 NativeRtlSdr.c
 
LOCAL_C_INCLUDES += \
jni/libusb \
jni/libusb/android \
jni/libusb/libusb \
jni/libusb/libusb/os \
jni/rtl-sdr/include \
jni/rtl-sdr/src \
jni/rtl-ais \
jni/rtl-ais/aisdecoder \
jni/rtl-ais/aisdecoder/lib

LOCAL_CFLAGS += -Wall -DLIBUSB_DESCRIBE="" -O3 -fno-builtin-printf -fno-builtin-fprintf
LOCAL_MODULE:= NativeRtlSdr
LOCAL_LDLIBS := -lm -llog

include $(BUILD_SHARED_LIBRARY)
#include $(BUILD_EXECUTABLE)
