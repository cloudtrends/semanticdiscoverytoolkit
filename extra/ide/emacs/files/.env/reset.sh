#!/bin/bash
#
# Script to manually reset vars in the current shell.
# NOTE: Source this using ". reset.sh"
#

unset PROJECTS_CLASSPATH;
unset CLASSPATH;
unset _DEV_VARS_READ;
unset LD_LIBRARY_PATH;
. .vars
. .classpath_def

echo "PROJECTS=$PROJECTS";
echo "CLASSPATH=$CLASSPATH";
