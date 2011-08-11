#include <stdio.h>
#include <stdlib.h>
#include "scwindowlib.h"

void simpletest() {
    GetWindowList();
    // printf("%%TEST_FAILED%% time=0 testname=simpletest (windowlibtest) message=some error message\n");
}

int main(int argc, char** argv) {
    (void) argc; // Unused.
    (void) argv; // Unused.

    printf("%%SUITE_STARTING%% windowlibtest\n");
    printf("%%SUITE_STARTED%%\n");

    printf("%%TEST_STARTED%% simpletest (windowlibtest)\n");
    simpletest();
    printf("%%TEST_FINISHED%% time=0 simpletest (windowlibtest)\n");

    printf("%%SUITE_FINISHED%% time=0\n");

    return (EXIT_SUCCESS);
}
