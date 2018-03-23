#!/bin/sh
#
# -------------------------------------------------------------------
# A script aims at indexing a sequence file.
# Copyright (c) - IFREMER Bioinformatics, 2018
# -------------------------------------------------------------------
# User manual:
#   https://github.com/ifremer-bioinformatics 
# -------------------------------------------------------------------
# A utility class to index a sequence file.
#  
#  
# Sample use:
# CmdLineIndexer -d <index-directory> -i tests/databank/fasta_prot/uniprot.faa
# Supported format: Embl, Genbank, Fasta
# Note: environment variables are accepted in file path.
# 
# For now, indexing must be done in the same directory as the input sequence
# file. Consider using symbolic link if your sequence file is located within a
# read-only directory.
#
# Note: environment variables are accepted in file path.
# 
# In addition, some parameters can be passed to the JVM for special 
# configuration purposes:
# -DKL_DEBUG=true ; if true, if set, log will be in debug mode
# -DKL_WORKING_DIR=an_absolute_path ; if not set, log and working 
#  directories are set to java.io.tmp
# -DKL_LOG_FILE=a_file_name ; if set, creates a log file with that 
#  name within KL_WORKING_DIR
# -DKL_CONF_DIR=an_absolute_path ; the absolute path to a home-made  
#  conf directory. If not set, use ${user.dir}/conf.
#

function help(){
  printf "\n$0: a tool to index a sequence file.\n\n"
  printf "usage: $0 [-h] -w <working-directory> -d <index-directory> -i <sequence-file> \n\n"
  exit 1
}

function error() {
  printf "ERROR: %s\n" "$*" >&2;
}

# *** Application home
KL_APP_HOME=$( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )

# *** Working directory for log file 
KL_WORKING_DIR=

# *** Sequence file to index; default is none
SEQ_FILE=

# *** Place to index data; default is none
INDEX_DIR=

while getopts hw:i:d: opt
do
    case "$opt" in
      i)  SEQ_FILE="$OPTARG";;
      d)  INDEX_DIR="$OPTARG";;
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
KL_JAVA_ARGS="-Xms1g -Xmx4g -DKL_HOME=$KL_APP_HOME -DKL_WORKING_DIR=$KL_WORKING_DIR"

# *** JARs section
KL_JAR_LIST_TMP=`\ls $KL_APP_HOME/bin/*.jar`
KL_JAR_LIST=`echo $KL_JAR_LIST_TMP | sed 's/ /:/g'`

if [  ! "$INDEX_DIR"  ]; then
  error "index directory not provided"
  exit 1
fi

if [  ! "$SEQ_FILE"  ]; then
  error "sequence file not provided"
  exit 1
fi

# Indexing of sequence file is done next to that file.
# To avoid polluting that place, we create a dedicated
# directory, put there a link to the sequence file to index
# then run indexing on that file/link.
mkdir -p $INDEX_DIR
if [ $? != 0  ]; then
  error "unable to create directory $INDEX_DIR"
  exit 1
fi

SEQ_FILE_NAME=$(basename "$SEQ_FILE")
SEQ_FILE_LINK=$INDEX_DIR/$SEQ_FILE_NAME

ln -s $SEQ_FILE $SEQ_FILE_LINK
if [ $? != 0  ]; then
  error "unable to create link from $SEQ_FILE to $SEQ_FILE_LINK"
  exit 1
fi

# *** start application
KL_APP_MAIN_CLASS=fr.ifremer.bioinfo.blast.CmdLineIndexer
$KL_JAVA_VM $KL_JAVA_ARGS -classpath $KL_JAR_LIST $KL_APP_MAIN_CLASS -i $SEQ_FILE_LINK

