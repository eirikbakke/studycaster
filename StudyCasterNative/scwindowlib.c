#include <stdio.h>
#include <windows.h>
#include "scwindowlib.h"

/* Win32 API window function references:
   http://msdn.microsoft.com/en-us/library/ms632595 
   http://msdn.microsoft.com/en-us/library/ms632599 */

enum {
  WINDOW_TEXT_BUF_SZ = 32768
};

static BOOL CALLBACK lpEnumFunc(HWND hWnd, LPARAM lParam) {
  if (!IsWindowVisible(hWnd))
    return TRUE;
  
  /* Don't bother using GetWindowTextLengthW() to calculate the buffer size;
  there's a potential race condition there anyway. */
  wchar_t lpWindowText[WINDOW_TEXT_BUF_SZ];
  int res = GetWindowTextW(hWnd, lpWindowText, WINDOW_TEXT_BUF_SZ - 1);
  lpWindowText[WINDOW_TEXT_BUF_SZ-1] = L'\0';
  fwprintf(stderr, L"WindowText: \"%s\"\n", lpWindowText);
  return TRUE;
}

void GetWindowList(void) {
  BOOL res = EnumWindows(lpEnumFunc, 0);
  fprintf(stderr, "EnumWindows returned %d.\n", res);
}
