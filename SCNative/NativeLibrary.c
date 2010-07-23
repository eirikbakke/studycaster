#include <windows.h>
#include <stdio.h>
#include <string.h>
#include "NativeLibrary.h"

static wchar_t *lastInterestingWindowTitle = NULL;

static HWND ancestorMatching(HWND win, const wchar_t *titleMustInclude) {
  if (win == NULL)
    return NULL;
  int len = GetWindowTextLengthW(win);
  if (len > 0) {
    wchar_t buf[len + 1];
    if (GetWindowTextW(win, buf, len + 1) > 0 && wcsstr(buf, titleMustInclude) != NULL)
      return win;
  }
  return ancestorMatching(GetParent(win), titleMustInclude);
}

static RECT getRelevantArea(const wchar_t *titleMustInclude) {
  RECT nullRect;
  memset(&nullRect, 0, sizeof(RECT));
  HWND win = ancestorMatching(GetForegroundWindow(), titleMustInclude);
  if (win != NULL) {
    int len = GetWindowTextLengthW(win);
    if (len > 0) {
      free(lastInterestingWindowTitle);
      lastInterestingWindowTitle = malloc((len + 1) * sizeof(wchar_t));
      // TODO: Is this right?
      if (GetWindowTextW(win, lastInterestingWindowTitle, len + 1) == 0) {
        free(lastInterestingWindowTitle);
        lastInterestingWindowTitle = NULL;
      }
    }
    RECT rect;
    if (GetWindowRect(win, &rect)) {
      return rect;
    } else {
      return nullRect;
    }
  } else {
    if (lastInterestingWindowTitle != NULL && wcsstr(lastInterestingWindowTitle, titleMustInclude) != NULL) {
      win = FindWindowW(NULL, lastInterestingWindowTitle);
      if (win != NULL) {
        HDC dc = GetDCEx(win, NULL, DCX_WINDOW | DCX_CLIPSIBLINGS);
        if (dc != NULL) {
          RECT rect;
          int gcb = GetClipBox(dc, &rect);
          ReleaseDC(win, dc);
          if (gcb == SIMPLEREGION)
            return rect;
        }
      }
    }
  }
  return nullRect;
}

JNIEXPORT void JNICALL Java_no_ebakke_studycaster2_NativeLibrary_getWindowArea_1internal(
  JNIEnv *env, jclass clazz, jstring titleMustInclude, jintArray result
) {
  const jchar *titleMustIncludeConv = (*env)->GetStringChars(env, titleMustInclude, NULL);
  RECT rect = getRelevantArea(titleMustIncludeConv);
  jint resbuf[4];
  resbuf[0] = rect.left;
  resbuf[1] = rect.top;
  resbuf[2] = rect.right  - rect.left;
  resbuf[3] = rect.bottom - rect.top;
  (*env)->SetIntArrayRegion(env, result, 0, 4, resbuf);
  // fwprintf(stderr, L"\"%s\": (%d,%d)-(%d,%d)\n", titleMustIncludeConv, rect.left, rect.top, rect.right, rect.bottom);
  //fflush(stderr);
  (*env)->ReleaseStringChars(env, titleMustInclude, titleMustIncludeConv);
}
