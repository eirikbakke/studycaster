#include <windows.h>
#include <stdio.h>
#include "NativeLibrary.h"

static BOOL CALLBACK myEnumWindowsProc(HWND hwnd, LPARAM lParam) {
  int len = GetWindowTextLength(hwnd);
  //printf("got this far, length was %d", len);
  if (len > 0) {
    char buf[len + 1];
    //printf("so %d", sizeof(*buf));
    if (GetWindowText(hwnd, buf, len + 1) > 0) {
      printf("The window text is \"%s\"\n", buf);
    }
  }

  return TRUE;
}

JNIEXPORT jstring JNICALL Java_no_ebakke_studycaster2_NativeLibrary_getTestString(JNIEnv *env, jclass clazz) {
  BOOL res = EnumWindows(&myEnumWindowsProc, (LPARAM) NULL);
  printf("EnumWindows returned %s\n", res ? "true" : "false");
  return (*env)->NewStringUTF(env, "Hello world from JNI (test string5)!");
}
