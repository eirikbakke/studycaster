/*
 * File:   newsimpletest.c
 * Author: Administrator
 *
 * Created on Aug 10, 2011, 5:08:38 PM
 */

#include <stdio.h>
#include <stdlib.h>
#include "windowlib.h"

void simpletest() {
    get_windows();
    // printf("%%TEST_FAILED%% time=0 testname=simpletest (windowlibtest) message=some error message\n");
}

int main(int argc, char** argv) {
    printf("%%SUITE_STARTING%% windowlibtest\n");
    printf("%%SUITE_STARTED%%\n");

    printf("%%TEST_STARTED%% simpletest (windowlibtest)\n");
    simpletest();
    printf("%%TEST_FINISHED%% time=0 simpletest (windowlibtest)\n");

    printf("%%SUITE_FINISHED%% time=0\n");

    return (EXIT_SUCCESS);
}
