#!/bin/sh
#
# Archive source code (for backup).
#
# arg1: path to directory containing source code.
#
# output: tarball in the current working directory.

path="$1";
name=`basename $path`;
date=`date +%Y\-%m\-%d`;

archive="$name.src.$date.tgz";
echo "Archiving '$path' to '$archive'..."
tar -czf "$archive" --exclude=build --exclude=.svn --exclude=*.bdb --exclude='*~' --exclude=modlib --exclude=localdist "$path"
