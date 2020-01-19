cmake_minimum_required(VERSION 3.6.0)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -DLIBUSB_DESCRIBE=\"\" -O3 -fno-builtin-printf -fno-builtin-fprintf")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Werror=pointer-to-int-cast -Werror=int-to-pointer-cast -Werror=shorten-64-to-32 -Werror=implicit-function-declaration")

find_library(
        log-lib
        log
)

add_library(
        # Specifies the name of the library.
        NativeRtlSdr

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to source file(s).
        libusb/libusb/core.c
        libusb/libusb/descriptor.c
        libusb/libusb/hotplug.c
        libusb/libusb/io.c
        libusb/libusb/sync.c
        libusb/libusb/strerror.c
        libusb/libusb/os/linux_usbfs.c
        libusb/libusb/os/poll_posix.c
        libusb/libusb/os/threads_posix.c
        libusb/libusb/os/linux_netlink.c
        rtl-sdr/src/rtl-sdr-android.c
        rtl-sdr/src/convenience/convenience.c
        rtl-sdr/src/tuner_e4k.c
        rtl-sdr/src/tuner_fc0012.c
        rtl-sdr/src/tuner_fc0013.c
        rtl-sdr/src/tuner_fc2580.c
        rtl-sdr/src/tuner_r82xx.c
        rtl-sdr/src/rtl_test.c
        rtl-ais/main.c
        rtl-ais/rtl_ais.c
        rtl-ais/aisdecoder/aisdecoder.c
        rtl-ais/aisdecoder/sounddecoder.c
        rtl-ais/aisdecoder/lib/receiver.c
        rtl-ais/aisdecoder/lib/protodec.c
        rtl-ais/aisdecoder/lib/hmalloc.c
        rtl-ais/aisdecoder/lib/filter.c
        rtl-ais/tcp_listener/tcp_listener.c
        NativeRtlSdrUtils.cpp
        NativeRtlSdr.cpp
)

# Specifies a path to native header files.
include_directories(
        .
        libusb
        libusb/android
        libusb/libusb
        libusb/libusb/os
        rtl-sdr/include
        rtl-sdr/src
        rtl-ais
        rtl-ais/aisdecoder
        rtl-ais/aisdecoder/lib
)

target_link_libraries(
        NativeRtlSdr
        ${log-lib}
)
