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
GREP=grep
NM=nm
CCADMIN=CCadmin
RANLIB=ranlib
CC=gcc.exe
CCC=g++
CXX=g++
FC=gfortran
AS=as.exe

# Macros
CND_PLATFORM=MinGW_32-Windows
CND_CONF=Debug
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

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
LDLIBSOPTIONS=-lgdi32

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libSCNative.dll

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libSCNative.dll: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	gcc.exe -Wl,--kill-at -shared -shared -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libSCNative.dll ${OBJECTFILES} ${LDLIBSOPTIONS} 

${OBJECTDIR}/NativeLibrary.o: NativeLibrary.c 
	${MKDIR} -p ${OBJECTDIR}
	${RM} $@.d
	$(COMPILE.c) -g -Wall -D_JNI_IMPLEMENTATION_ -I/C/Program\ Files\ \(x86\)/Java/jdk1.5.0_22/include -I/C/Program\ Files\ \(x86\)/Java/jdk1.5.0_22/include/win32  -MMD -MP -MF $@.d -o ${OBJECTDIR}/NativeLibrary.o NativeLibrary.c

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libSCNative.dll

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
