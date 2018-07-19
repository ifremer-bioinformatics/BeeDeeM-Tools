#!/bin/sh
#
# -------------------------------------------------------------------
# A script aims at running PLAST.
# Copyright (c) - IFREMER Bioinformatics, 2018
# -------------------------------------------------------------------
# User manual:
#   https://github.com/ifremer-bioinformatics 
# -------------------------------------------------------------------
# A utility class to query a sequence index.
#  
# Sample uses:
# -p plastp -i tests/databank/plast/query.fa -d tests/databank/plast/tursiops.fa -o ztest.xml -a 8 -maxhits 5 -maxhsps 1 -e 1e-5 -F F
# 
# Use program with -h argument to get help.
# Note: environment variables are accepted in file path.
# 
# In addition, some parameters can be passed to the JVM for special 
# configuration purposes:
# -DKL_DEBUG=true ; if true, if set, log will be in debug mode
# -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working 
#  directories are set to java.io.tmp
# -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that 
#  name within KL_WORKING_DIR
#

function help(){
  printf "\n$0: a tool to run PLAST.\n\n"
  printf "usage: $0 [-h] -w <working-directory> -p <plast-program> -i <query-file> -d <bank-file> -o <result-file> \n"
  exit 1
}

function error() {
  printf "ERROR: %s\n" "$*" >&2;
}

# *** Application home
KL_APP_HOME=$( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )

# *** Working directory for log file 
KL_WORKING_DIR=

# *** Handle part of cmdline arguments
while getopts hw: opt
do
    case "$opt" in
      w)  KL_WORKING_DIR="$OPTARG";;
      h)  help;;
      \?) help;;
    esac
done
shift `expr $OPTIND - 1`

# *** Working directory
if [  ! "$KL_WORKING_DIR"  ]; then
  error "working directory not provided"
  exit 1
fi
mkdir -p $KL_WORKING_DIR
if [ $? != 0  ]; then
  error "unable to create directory $KL_WORKING_DIR"
  exit 1
fi

# *** Java VM 
KL_JAVA_VM=java
KL_JAVA_ARGS="-Xms1g -Xmx4g -DKL_HOME=$KL_APP_HOME -DKL_WORKING_DIR=$KL_WORKING_DIR -Djava.library.path=$KL_APP_HOME/native"

# *** JARs section
KL_JAR_LIST_TMP=`\ls $KL_APP_HOME/bin/*.jar`
KL_JAR_LIST=`echo $KL_JAR_LIST_TMP | sed 's/ /:/g'`

# *** start application
KL_APP_MAIN_CLASS=fr.ifremer.bioinfo.plast.PlastRunner
$KL_JAVA_VM $KL_JAVA_ARGS -classpath $KL_JAR_LIST $KL_APP_MAIN_CLASS $@

