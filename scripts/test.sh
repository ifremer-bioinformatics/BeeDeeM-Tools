#!/bin/sh
#
# -------------------------------------------------------------------
# A script aims at querying a sequence index.
# Copyright (c) - IFREMER Bioinformatics, 2018
# -------------------------------------------------------------------
# User manual:
#   https://gitlab.ifremer.fr/bioinfo/BeeDeeM-Tools 
# -------------------------------------------------------------------
# A script to test BDM-Tools installation.
#  
# Sample uses:
#   test.sh -w $SCRATCH
#

function help(){
  printf "\n$0: a tool to test BeeDeeM-Tools using './data' directory.\n\n"
  printf "usage: $0 [-h] -w <working-directory> \n\n"
  exit 1
}

function error() {
  printf "ERROR: %s\n" "$*" >&2;
}

# *** Application home
KL_APP_HOME=$( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )

# *** Sequence file to index; default is none
SEQ_NAME="tursiops.fa"
IDX_NAME="${SEQ_NAME}.ld"
SEQ_FILE="$KL_APP_HOME/data/$SEQ_NAME"

# *** Place to index data
INDEX_DIR=tursiops

# *** Working directory for log file 
KL_WORKING_DIR=

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

# *** TEST 1/5: index a sequence file
echo "> 1/5: Running 'index.sh' tool:"
if [ -d $KL_WORKING_DIR/$INDEX_DIR/${IDX_NAME}  ]; then
  echo "   skip: already done"
else
  CMD="$KL_APP_HOME/index.sh -w $KL_WORKING_DIR -d $KL_WORKING_DIR/$INDEX_DIR -i $SEQ_FILE"
  echo "$CMD"
  eval $CMD
  if [ $? != 0  ]; then
    error "unable to run 'index.sh'"
    exit 1
  else
    echo "  OK"
  fi
fi

# *** TEST 2/5: query the created index
echo "> 2/5: Running 'query.sh' tool:"
CMD="$KL_APP_HOME/query.sh -w $KL_WORKING_DIR -d $KL_WORKING_DIR/$INDEX_DIR/${IDX_NAME} -i ENSTTRP00000003887,ENSTTRP00000007204,ENSTTRP00000007202 > $KL_WORKING_DIR/bdm-query-test.fa"
echo "$CMD"
eval $CMD
if [ $? != 0  ]; then
  error "unable to run 'query.sh'"
  exit 1
else
  echo "  OK"
fi

# *** TEST 3/5: cut a sequence into slices of 1000 sequences each
# Prepare a symlink to the file to cut: it can be located in a place
# having only read access... and the 'cut' tool writes new files next
# to input file
if [ ! -L "$KL_WORKING_DIR/$SEQ_NAME" ]; then
  ln -s $SEQ_FILE $KL_WORKING_DIR/$SEQ_NAME
  if [ $? != 0  ]; then
    error "unable to create link from $SEQ_FILE to $KL_WORKING_DIR/$SEQ_NAME"
    exit 1
  fi
fi

echo "> 3/5 Running 'cut.sh' tool:"
CMD="$KL_APP_HOME/cut.sh -w $KL_WORKING_DIR -i $KL_WORKING_DIR/$SEQ_NAME -p 1000"
echo "$CMD"
eval $CMD
if [ $? != 0  ]; then
  error "unable to run 'query.sh'"
  exit 1
else
  echo "  OK"
fi

# *** TEST 4/5: run PLASTp
echo "> 4/5 Running 'plast.sh' tool:"
export KL_WORKING_DIR=$KL_WORKING_DIR
CMD="$KL_APP_HOME/plast.sh -p plastp -i $KL_APP_HOME/data/query.fa -d $KL_APP_HOME/data/tursiops.fa -o $KL_WORKING_DIR/bdm-tools-plast.xml -a 8 -maxhits 5 -maxhsps 1 -e 1e-5 -F F"
echo "$CMD"
eval $CMD
if [ $? != 0  ]; then
  error "unable to run 'plast.sh'"
  exit 1
else
  echo "  OK"
fi

# *** TEST 5/5: run CSV export
echo "> 5/5 Running 'dumpcsv.sh' tool:"
CMD="$KL_APP_HOME/dumpcsv.sh -i $KL_WORKING_DIR/bdm-tools-plast.xml -o $KL_WORKING_DIR/bdm-tools-plast.csv -bho"
echo "$CMD"
eval $CMD
if [ $? != 0  ]; then
  error "unable to run 'dumpcsv.sh'"
  exit 1
else
  echo "  OK"
fi

