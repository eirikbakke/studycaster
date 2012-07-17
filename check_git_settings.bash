#!/bin/bash

AUTHORS=Authors.txt
cd `dirname "${BASH_SOURCE[0]}"`

FOUND_AUTHOR=$(
  IFS="	" # Tab
  CONF_NAME=`git config --get user.name`
  CONF_EMAIL=`git config --get user.email`
  cat $AUTHORS | while read NAME EMAIL; do
    if [ "$CONF_NAME" == "$NAME" -a "$CONF_EMAIL" == "$EMAIL" ]; then
      echo 1
    fi
  done
)

INCORRECT_SETTINGS=
confirm_setting () {
  VALUE=`git config --get $1`
  if [ "$VALUE" != "$2" ]; then
    echo "Please use \"git config ${1} ${2}\"."
    INCORRECT_SETTINGS=1
  fi
}
if [[ "`uname -a`" == *CYGWIN* ]]; then
  confirm_setting core.fileMode false
fi
confirm_setting core.autocrlf false
confirm_setting core.safecrlf true
confirm_setting core.eol lf

if [ -z "$FOUND_AUTHOR" ]; then
  echo "Please use \"git config user.[name|email]\", or add a new entry to the \"${AUTHORS}\" file."
  exit 1
fi
if [ "$INCORRECT_SETTINGS" ]; then
  exit 1
fi

