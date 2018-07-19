#!/bin/sh
#
# -------------------------------------------------------------------
# A script aims at querying a sequence index.
# Copyright (c) - IFREMER Bioinformatics, 2018
# -------------------------------------------------------------------
# User manual:
#   https://github.com/ifremer-bioinformatics 
# -------------------------------------------------------------------
# A utility class to query a sequence index.
#  
# Sample uses:
# CmdLineUserQuery -d tests/databank/fasta_prot/uniprot.faa.ld -i M4K2_HUMAN
#   -> retrieve sequence M4K2_HUMAN from index
# CmdLineUserQuery -d tests/databank/fasta_prot/uniprot.faa.ld -c -i M4K2_HUMAN
#   -> retrieve complement of sequence M4K2_HUMAN from index, i.e. retrieve ALL
#      sequences BUT M4K2_HUMAN
# CmdLineUserQuery -d tests/databank/fasta_prot/uniprot.faa.ld -f tests/databank/fasta_prot/fo-seqids.txt
#   -> retrieve from index sequence(s) identified from IDs contained in file
#      fo-seqids.txt 
# 
# To retrieve list of sequence IDs (hit IDs) from a tabular BLAST result
# (-outfmt 4), simply use the following command on a Unix system :
# cut -f 1 blast-result.tab | sort | uniq > fo-seqids.txt
# 
# Use program without any arguments to get help.
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
  printf "\n$0: a tool to query a sequence index.\n\n"
  printf "usage: $0 [-h] -w <working-directory> -d <sequence-file> [-c] -i <ID>  \n"
  printf "or \n"
  printf "usage: $0 [-h] -w <working-directory> -d <sequence-file> [-c] -f <FileOfIds>  \n\n"
  exit 1
}

function error() {
  printf "ERROR: %s\n" "$*" >&2;
}

# *** Application home
KL_APP_HOME=$( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )

# *** Working directory for log file 
KL_WORKING_DIR=
INDEX_DIR=
QUERY_IDS=
F_OF_IDS=
GET_COMPLEMENT=0

# *** Handle cmdline arguments
while getopts hcf:i:w:d: opt
do
    case "$opt" in
      i)  QUERY_IDS="$OPTARG";;
      f)  F_OF_IDS="$OPTARG";;
      c)  GET_COMPLEMENT=1;;
      w)  KL_WORKING_DIR="$OPTARG";;
      d)  INDEX_DIR="$OPTARG";;
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

# *** Prepare a valid cmd-line for CmdLineUserQuery tool
CMD_ARGS=" -d $INDEX_DIR"
if [ "$QUERY_IDS"  ]; then
  CMD_ARGS+=" -i $QUERY_IDS"
elif [ "$F_OF_IDS"  ]; then
  CMD_ARGS+=" -f $F_OF_IDS"
else
  error "provide either IDs (-i)  or file of IDs (-f)"
  exit 1
fi

if [ $GET_COMPLEMENT -eq 1  ]; then
  CMD_ARGS+=" -c"
fi

# *** start application
KL_APP_MAIN_CLASS=fr.ifremer.bioinfo.bdm.tools.CmdLineUserQuery
$KL_JAVA_VM $KL_JAVA_ARGS -classpath $KL_JAR_LIST $KL_APP_MAIN_CLASS $CMD_ARGS

