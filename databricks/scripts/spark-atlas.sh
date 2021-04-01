#!/bin/bash

##############################################################################
# Description: init script for databricks cluster
# Usage: upload to dbfs, configure init script in cluster advanced options

# Reference:
# https://github.com/dotnet/spark/blob/main/deployment/install-worker.sh
##############################################################################

SCRIPT_ROOT=/dbfs/databricks/scripts
LOGFILE=$SCRIPT_ROOT/logfile.txt

# redirect stdout/stderr to a file
# enable cluster logging as well. this is just backup
exec &> $LOGFILE

echo "HELLO WORLD"

# JAR PATHS
INPUT_JARS=$SCRIPT_ROOT/jars/*
JAR_PATH=/databricks/jars

cp -vR $INPUT_JARS $JAR_PATH
echo "=== finished copying jars to $JAR_PATH"

# ATLAS PROPERTIES PATHS
SPARK_CONF=/databricks/spark/conf/
ATLAS_PROPERTIES_FILE=atlas-application.properties
INPUT_ATLAS_PROPERTIES=$SCRIPT_ROOT/$ATLAS_PROPERTIES_FILE

cp -v $INPUT_ATLAS_PROPERTIES $SPARK_CONF
echo "=== finished copying atlas properties to $SPARK_CONF"
