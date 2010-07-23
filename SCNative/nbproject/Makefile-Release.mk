#
# Generated Makefile - do not edit!
#
# Edit the Makefile in the project folder instead (../Makefile). Each target
# has a -pre and a -post target defined where you can add customized code.
#
# This makefile implements configuration specific macros and targets.


# Environment
MKDIR=mkdir
CP=cp
CCADMIN=CCadmin
RANLIB=ranlib
CC=gcc.exe
CCC=g++.exe
CXX=g++.exe
FC=
AS=as.exe

# Macros
CND_PLATFORM=MinGW_TDM-Windows
CND_CONF=Release
CND_DISTDIR=dist

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=build/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/NativeLibrary.o

# C Compiler Flags
CFLAGS=-m32

# CC Compiler Flags
CCFLAGS=
CXXFLAGS=

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	${MAKE}  -f nbproject/Makefile-Release.mk dist/Release/MinGW_TDM-Windows/libSCNative.dll

dist/Release/MinGW_TDM-Windows/libSCNative.dll: ${OBJECTFILES}
	${MKDIR} -p dist/Release/MinGW_TDM-Windows
	gcc.exe -Wl,--kill-at -shared -shared -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libSCNative.dll ${OBJECTFILES} ${LDLIBSOPTIONS} 

${OBJECTDIR}/NativeLibrary.o: nbproject/Makefile-${CND_CONF}.mk NativeLibrary.c 
	${MKDIR} -p ${OBJECTDIR}
	${RM} $@.d
	$(COMPILE.c) -O2 -Wall -D_JNI_IMPLEMENTATION_ -I/C\Program\ Files\Java\jdk1.6.0_17\include -I/C\Program\ Files\Java\jdk1.6.0_17\include\win32  -MMD -MP -MF $@.d -o ${OBJECTDIR}/NativeLibrary.o NativeLibrary.c

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r build/Release
	${RM} dist/Release/MinGW_TDM-Windows/libSCNative.dll

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
