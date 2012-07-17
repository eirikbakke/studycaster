#!/bin/bash

# The following rewrite was run on 2012-07-17 (with the version of this script
# as of then). The old version of the repository has been kept around under the
# name "studycaster2-before-rewrite".
#
# git filter-branch \
#  --env-filter 'export GIT_AUTHOR_EMAIL="ebakke@mit.edu"; export GIT_AUTHOR_NAME="Eirik Bakke"' \
#  --tree-filter '/home/ebakke/fix_windows.bash'

set -e

find -type d -print0 | xargs -0r chmod 755
find -type f -print0 | xargs -0r chmod 644
find -type f -name "*.bash" -print0 | xargs -0r chmod 754

for EXT in `gawk 'match($0, /^\*?([.a-zA-Z0-9]+) *text *$/, a) {print a[1]}' < .gitattributes`
do
  sh -c "find -name \"*${EXT}\" -print0 | xargs -0r -- dos2unix --" 2>&1 \
    | grep -v "dos2unix: converting file" || true
done

