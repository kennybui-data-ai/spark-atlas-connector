#!/bin/bash

# redirect stdout/stderr to a file
SCRIPT_ROOT=/dbfs/databricks/scripts
LOG_PATH=$SCRIPT_ROOT/logfile.txt
# sudo rm -f $LOG_PATH
# sudo touch $LOG_PATH
exec &> $LOG_PATH
# exec >> $LOG_PATH
# exec 2>&1

echo "HELLO WORLD"
