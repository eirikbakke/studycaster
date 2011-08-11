#include <stdio.h>
#include <windows.h>
#include "scwindowlib.h"

/* Win32 API window function references:
   http://msdn.microsoft.com/en-us/library/ms632595 
   http://msdn.microsoft.com/en-us/library/ms632599 */

enum {
  /* Don't bother using GetWindowTextLengthW() to calculate the buffer size;
  there's a potential race condition there anyway. */
  WINDOW_TEXT_BUF_SZ = 32768
};

static BOOL CALLBACK lpEnumFunc(HWND hWnd, LPARAM lParam) {
  wchar_t lpWindowText[WINDOW_TEXT_BUF_SZ];
  int res = 0;

  if (!IsWindowVisible(hWnd))
    return TRUE;

  res = GetWindowTextW(hWnd, lpWindowText, WINDOW_TEXT_BUF_SZ - 1);
  lpWindowText[WINDOW_TEXT_BUF_SZ-1] = L'\0';
  fwprintf(stderr, L"WindowText: \"%s\"\n", lpWindowText);

  return TRUE;
}

void GetWindowList(void) {
  /* Enumerate top-level windows, i.e. what non-Win32 programmers would just
  call "windows" or "frames" (as opposed to child windows, which include things
  like labels and button controls). Assume that EnumWindows() always does this
  enumeration in Z-order. While not directly guaranteed by the API
  documentation, it seems like a fairly safe assumption, see
  http://stackoverflow.com/questions/295996 . If this breaks in the future, a
  workaround may involve using the GetWindow() function in conjunction with
  EnumWindows() to manually sort the enumerated windows. Don't do the
  enumeration itself using GetWindow(), though; see the warnings about this in
  the API documentation. */
  BOOL res = EnumWindows(lpEnumFunc, 0);
  fprintf(stderr, "EnumWindows returned %d.\n", res);
}
