#!/bin/sh
#
# -------------------------------------------------------------------
# A script aims at running PLAST.
# Copyright (c) - IFREMER Bioinformatics, 2018
# -------------------------------------------------------------------
# User manual:
#   https://gitlab.ifremer.fr/bioinfo/BeeDeeM-Tools 
# -------------------------------------------------------------------
# A script to run PLAST.
#  
# Sample uses:
# plast.sh -p plastp -i tests/databank/plast/query.fa -d tests/databank/plast/tursiops.fa -o ztest.xml -a 8 -maxhits 5 -maxhsps 1 -e 1e-5 -F F
# 
# In addition, set a multi-thread safe working directory using
# the following environment variable:
#   KL_WORKING_DIR=an_absolute_path 
#  if not set, log and working directories are set to /tmp/plast
#  (each PLAST job should write to its own KL_WORKING_DIR)
#
# Use program with -h argument to get help.
# Note: environment variables are accepted in file path.
#

function help(){
  printf "\n$0: a tool to run PLAST.\n\n"
  printf "usage: $0 [-h] \n\n"
  printf "required arguments are: -p <plast-program> -i <query-file> -d <bank-file> -o <result-file> \n"
  printf "   -p: plastp, plastx, plastn, tplastx, tplastn \n"
  printf "   -i: query file (must be FASTA)\n"
  printf "   -d: reference bank (FASTA file or BLAST bank)\n"
  printf "   -o: output file (NCBI XML)\n\n"
  printf "optional arguments are: -a <cores> -maxhits <num> -maxhsps <num> -e <num> -F <T|F> \n"
  printf "optional arguments default values: \n" 
  printf "   -a:       all cores available\n"
  printf "   -maxhits: all hits \n"
  printf "   -maxhsps: all HSPs\n"
  printf "   -e      : 10 (evalue threshold)\n"
  printf "   -F      : T (filter query for low complexity)\n\n"
  exit 1
}

function error() {
  printf "ERROR: %s\n" "$*" >&2;
}

# *** Application home
KL_APP_HOME=$( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )

# *** Working directory for log file 
if [  ! "$KL_WORKING_DIR"  ]; then
  KL_WORKING_DIR=/tmp/plast
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
