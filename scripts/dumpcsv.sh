#!/bin/sh
#
# -------------------------------------------------------------------
# A script aims at exporting BLAST or PLAST results (legacy XML) as CSV.
# Copyright (c) - IFREMER Bioinformatics, 2018
# -------------------------------------------------------------------
# User manual:
#   https://gitlab.ifremer.fr/bioinfo/BeeDeeM-Tools 
# -------------------------------------------------------------------
# A script to run CSV Export tool.
#  
# Sample uses:
# dumpcsv.sh -i tests/datafile/hits_only.xml -o ztest.csv
# 
# Use program with -h argument to get help.
# Note: environment variables are accepted in file path.
#
# A log file called TextDump.log is created within ${java.io.tmpdir}.
# This default log file can be redirected using JRE variables KL_WORKING_DIR
# and KL_LOG_FILE. E.g. java ... -DKL_WORKING_DIR=/my-path -DKL_LOG_FILE=query.log
# or you can also do "export KL_WORKING_DIR=..." before calling this script.
# 
# In addition, some parameters can be passed to the JVM for special 
# configuration purposes:
# -DKL_DEBUG=true ; if true, if set, log will be in debug mode
#

function help(){
  printf "\n$0: a tool to export BLAST or PLAST results (legacy XML) as CSV.\n\n"
  printf "usage: $0 [-h] \n\n"
  printf "required argument is: -i <xml-result-file> \n"
  printf "   -i: a BLAST or PLAST result file (must be legacy NCBI BLAST XML format)\n"
  printf "optional arguments are: -o <csv-file> -c <column-ids> -tax <taxonomy-index> -go <GO-index> -ipr <InterPro-index> -ec <Enzyme-index> \n"
  printf "optional arguments default values: \n" 
  printf "   -o      : none. Tool dumps CSV on stdout \n"
  printf "   -c      : 0,1,2,7,4,6,10,11,12,14,16,17,19\n"
  printf "   -tax    : none. Otherwise provide a path to BeeDeeM-based index to NCBI Taxonomy \n"
  printf "   -ec     : none. Otherwise provide a path to BeeDeeM-based index to Enzyme \n"
  printf "   -ipr    : none. Otherwise provide a path to BeeDeeM-based index to InterPro \n"
  printf "   -go     : none. Otherwise provide a path to BeeDeeM-based index to GeneOntology \n"
  exit 1
}

function error() {
  printf "ERROR: %s\n" "$*" >&2;
}

# *** Application home
KL_APP_HOME=$( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )

# *** Working directory for log file 
if [  ! "$KL_WORKING_DIR"  ]; then
  KL_WORKING_DIR=/tmp
fi
mkdir -p $KL_WORKING_DIR
if [ $? != 0  ]; then
  error "unable to create directory $KL_WORKING_DIR"
  exit 1
fi

# *** Java VM 
KL_JAVA_VM=java
KL_JAVA_ARGS="-Xms2g -Xmx32g -DKL_HOME=$KL_APP_HOME -DKL_WORKING_DIR=$KL_WORKING_DIR"

# *** JARs section
KL_JAR_LIST_TMP=`\ls $KL_APP_HOME/bin/*.jar`
KL_JAR_LIST=`echo $KL_JAR_LIST_TMP | sed 's/ /:/g'`

# *** start application
KL_APP_MAIN_CLASS=fr.ifremer.bioinfo.bdm.tools.CmdLineDumper
$KL_JAVA_VM $KL_JAVA_ARGS -classpath $KL_JAR_LIST $KL_APP_MAIN_CLASS $@
