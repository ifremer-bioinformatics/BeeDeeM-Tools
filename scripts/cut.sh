#!/bin/sh
#
# -------------------------------------------------------------------
# A script aims at cutting sequence file.
# Copyright (c) - IFREMER Bioinformatics, 2018
# -------------------------------------------------------------------
# User manual:
#   https://gitlab.ifremer.fr/bioinfo/BeeDeeM-Tools 
# -------------------------------------------------------------------
# A script to cut a sequence file.
#  
# Sample uses: 
# cut.sh -i tests/databank/fasta_prot/uniprot.faa -k fa -f 3
# to get 3rd sequence up to the end of input file
# -> result file will be here: tests/databank/fasta_prot/uniprot_3-end.faa
#
# CmdLineCutter -i tests/databank/fasta_prot/uniprot.faa -k fa -p 5
# cut input file into several parts, each of them containing 5 sequences
# -> result files will be (sample source file contains 10 sequences): 
# a. tests/databank/fasta_prot/uniprot_1-5.faa
# b. tests/databank/fasta_prot/uniprot_6-10.faa
#  
# Use program without any arguments to get help.
# Note: environment variables are accepted in file path.
# 
# A log file called CutSequenceFile.log is created within ${java.io.tmpdir}.
# This default log file can be redirected using JRE variables KL_WORKING_DIR
# and KL_LOG_FILE. E.g. java ... -DKL_WORKING_DIR=/my-path -DKL_LOG_FILE=query.log
#
# In addition, some parameters can be passed to the JVM for special 
# configuration purposes:
# -DKL_DEBUG=true ; if true, if set, log will be in debug mode
#

function help(){
  printf "\n$0: a tool to cut a sequence file.\n\n"
  printf "Note: new files are created next to input sequence file.\n\n"
  printf "usage: $0 [-h] -w <working-directory> -i <sequence-file> -k <format> -f 3\n"
  printf "        get 3rd sequence up to the end of input file \n\n"
  printf "usage: $0 [-h] -w <working-directory> -i <sequence-file> -k <format> -f 5 -t 10\n"
  printf "        get 3rd sequence up to the 10th sequence of input file \n\n"
  printf "usage: $0 [-h] -w <working-directory> -i <sequence-file> -k <format> -p 5\n"
  printf "        cut input file into several parts, each of them containing 5 sequences \n\n"
  printf "       <format>: one of fa, fq, gb, em\n\n"
  exit 1
}

function error() {
  printf "ERROR: %s\n" "$*" >&2;
}

# *** Application home
KL_APP_HOME=$( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )

# *** Working directory for log file 
KL_WORKING_DIR=

# *** Sequence file to cut; default is none
SEQ_FILE=

# *** Sequence file format; default is FASTA
SEQ_FORMAT="fa"

ARGS_LINE=" "

# *** Handle cmdline arguments
while getopts hw:i:f:t:p: opt
do
    case "$opt" in
      p)  ARGS_LINE="$ARGS_LINE -p $OPTARG";;
      f)  ARGS_LINE="$ARGS_LINE -f $OPTARG";;
      t)  ARGS_LINE="$ARGS_LINE -t $OPTARG";;
      i)  SEQ_FILE="$OPTARG";;
      w)  KL_WORKING_DIR="$OPTARG";;
      f)  SEQ_FORMAT="$OPTARG";;
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

if [  ! "$SEQ_FILE"  ]; then
  error "sequence file not provided"
  exit 1
fi

# *** Java VM 
KL_JAVA_VM=java
KL_JAVA_ARGS="-Xms1g -Xmx4g -DKL_HOME=$KL_APP_HOME -DKL_WORKING_DIR=$KL_WORKING_DIR"

# *** JARs section
KL_JAR_LIST_TMP=`\ls $KL_APP_HOME/bin/*.jar`
KL_JAR_LIST=`echo $KL_JAR_LIST_TMP | sed 's/ /:/g'`

# *** start application
KL_APP_MAIN_CLASS=fr.ifremer.bioinfo.bdm.tools.CmdLineCutter
$KL_JAVA_VM $KL_JAVA_ARGS -classpath $KL_JAR_LIST $KL_APP_MAIN_CLASS -d $KL_WORKING_DIR -i $SEQ_FILE -k $SEQ_FORMAT $ARGS_LINE

