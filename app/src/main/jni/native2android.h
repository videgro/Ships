#ifndef NATIVE_2_JAVA_H_
#define NATIVE_2_JAVA_H_

#include <android/log.h>

void send_message(const char *format, ...);

void send_message_err(const char *format, ...);

void send_exception(const int exception_code);

void send_ready();

//void thread_detach();

#endif
