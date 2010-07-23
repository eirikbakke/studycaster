#include <windows.h>
#include <stdio.h>
#include <string.h>
#include "NativeLibrary.h"

static wchar_t *lastInterestingWindowTitle = NULL;

static HWND ancestorMatching(HWND win, const wchar_t *titleMustInclude, const wchar_t **taboo) {
  if (win == NULL)
    return NULL;
  int len = GetWindowTextLengthW(win);
  if (len > 0) {
    wchar_t buf[len + 1];
    if (GetWindowTextW(win, buf, len + 1) > 0) {
      if (wcsstr(buf, titleMustInclude) != NULL) {
        return win;
      }
      int i;
      for (i = 0; taboo[i] != NULL; i++) {
        if (wcsstr(buf, taboo[i]) != NULL)
          return NULL;
      }
    }
  }
  return ancestorMatching(GetParent(win), titleMustInclude, taboo);
}

static RECT getPermittedArea(const wchar_t *titleMustInclude, const wchar_t **taboo) {
  RECT nullRect;
  memset(&nullRect, 0, sizeof(RECT));
  HWND win = ancestorMatching(GetForegroundWindow(), titleMustInclude, taboo);
  if (win != NULL) {
    int len = GetWindowTextLengthW(win);
    if (len > 0) {
      free(lastInterestingWindowTitle);
      lastInterestingWindowTitle = malloc((len + 1) * sizeof(wchar_t));
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
          if (gcb == SIMPLEREGION) {
            RECT offsetRect;
            if (GetWindowRect(win, &offsetRect)) {
              rect.left   += offsetRect.left;
              rect.top    += offsetRect.top;
              rect.right  += offsetRect.left;
              rect.bottom += offsetRect.top;
              return rect;
            } else {
              return nullRect;
            }
          }
        }
      }
    }
  }
  return nullRect;
}

JNIEXPORT void JNICALL Java_no_ebakke_studycaster2_NativeLibrary_getWindowArea_1internal(
  JNIEnv *env, jclass clazz, jstring titleMustInclude, jobjectArray taboo, jintArray result
) {
  int numTaboo = (*env)->GetArrayLength(env, taboo);
  const wchar_t *tabooConv[numTaboo + 1];
  int i;
  for (i = 0; i < numTaboo; i++)
    tabooConv[i] = (*env)->GetStringChars(env, (*env)->GetObjectArrayElement(env, taboo, i), NULL);
  tabooConv[numTaboo] = NULL;
  const jchar *titleMustIncludeConv = (*env)->GetStringChars(env, titleMustInclude, NULL);

  RECT rect = getPermittedArea(titleMustIncludeConv, tabooConv);

  jint resbuf[4];
  resbuf[0] = rect.left;
  resbuf[1] = rect.top;
  resbuf[2] = rect.right  - rect.left;
  resbuf[3] = rect.bottom - rect.top;
  (*env)->SetIntArrayRegion(env, result, 0, 4, resbuf);
  //fwprintf(stderr, L"\"%s\": (%d,%d)-(%d,%d)\n", titleMustIncludeConv, rect.left, rect.top, rect.right, rect.bottom);
  //fflush(stderr);
  (*env)->ReleaseStringChars(env, titleMustInclude, titleMustIncludeConv);
  for (i = 0; i < numTaboo; i++)
    (*env)->ReleaseStringChars(env, (*env)->GetObjectArrayElement(env, taboo, i), tabooConv[i]);
}
