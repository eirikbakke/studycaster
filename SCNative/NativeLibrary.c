#include <windows.h>
#include <stdio.h>
#include <string.h>
#include "NativeLibrary.h"

static wchar_t *lastInterestingWindowTitle = NULL;

static BOOL stringInList(const wchar_t *s, const wchar_t **list) {
  int i;
  for (i = 0; list[i] != NULL; i++) {
    if (wcsstr(s, list[i]) != NULL)
      return TRUE;
  }
  return FALSE;
}

static HWND ancestorMatching(HWND win, const wchar_t **whiteList, const wchar_t **blackList) {
  if (win == NULL)
    return NULL;
  int len = GetWindowTextLengthW(win);
  if (len > 0) {
    wchar_t buf[len + 1];
    if (GetWindowTextW(win, buf, len + 1) > 0) {
      if (stringInList(buf, blackList))
        return NULL;
      if (stringInList(buf, whiteList))
        return win;
    }
  }
  return ancestorMatching(GetParent(win), whiteList, blackList);
}

static RECT getPermittedArea(const wchar_t **whiteList, const wchar_t **blackList) {
  RECT nullRect;
  memset(&nullRect, 0, sizeof(RECT));
  HWND fgnd = GetForegroundWindow();
  HWND win = ancestorMatching(fgnd, whiteList, blackList);
  if (win != NULL) {
    win = fgnd; // Use the foreground child window instead of the actually matched window.
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
    if (lastInterestingWindowTitle != NULL && stringInList(lastInterestingWindowTitle, whiteList)) {
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

JNIEXPORT void JNICALL Java_no_ebakke_studycaster_screencasting_NativeLibrary_getPermittedRecordingArea_1internal(
  JNIEnv *env, jclass clazz, jobjectArray whiteList, jobjectArray blackList, jintArray result
) {
  int i;
  int numBlackList = (*env)->GetArrayLength(env, blackList);
  const wchar_t *blackListConv[numBlackList + 1];
  for (i = 0; i < numBlackList; i++)
    blackListConv[i] = (*env)->GetStringChars(env, (*env)->GetObjectArrayElement(env, blackList, i), NULL);
  blackListConv[numBlackList] = NULL;
  
  int numWhiteList = (*env)->GetArrayLength(env, whiteList);
  const wchar_t *whiteListConv[numWhiteList + 1];
  for (i = 0; i < numWhiteList; i++)
    whiteListConv[i] = (*env)->GetStringChars(env, (*env)->GetObjectArrayElement(env, whiteList, i), NULL);
  whiteListConv[numWhiteList] = NULL;

  RECT rect = getPermittedArea(whiteListConv, blackListConv);

  jint resbuf[4];
  resbuf[0] = rect.left;
  resbuf[1] = rect.top;
  resbuf[2] = rect.right  - rect.left;
  resbuf[3] = rect.bottom - rect.top;
  (*env)->SetIntArrayRegion(env, result, 0, 4, resbuf);
  //fwprintf(stderr, L"\"%s\": (%d,%d)-(%d,%d)\n", whiteListConv, rect.left, rect.top, rect.right, rect.bottom);
  //fflush(stderr);
  for (i = 0; i < numBlackList; i++)
    (*env)->ReleaseStringChars(env, (*env)->GetObjectArrayElement(env, blackList, i), blackListConv[i]);
  for (i = 0; i < numWhiteList; i++)
    (*env)->ReleaseStringChars(env, (*env)->GetObjectArrayElement(env, whiteList, i), whiteListConv[i]);

}
