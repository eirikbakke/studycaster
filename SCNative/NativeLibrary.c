#include "NativeLibrary.h"

JNIEXPORT jstring JNICALL Java_no_ebakke_studycaster2_NativeLibrary_getTestString(JNIEnv *env, jclass clazz) {
  return (*env)->NewStringUTF(env, "Hello world from JNI (test string4)!");
}
