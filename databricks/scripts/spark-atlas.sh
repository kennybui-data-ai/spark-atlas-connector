#!/bin/bash
# redirect stdout/stderr to a file
LOG_PATH=/dbfs/databricks/scripts/logfile.txt
# sudo rm -f $LOG_PATH
exec &> $LOG_PATH

# echo "hello world"
# echo "SPARK_CONF_DIR: $SPARK_CONF_DIR"

########################
# JARS
########################
INPUT_JARS=/dbfs/databricks/scripts/jars/.
JAR_PATH=/databricks/jars/
# NOTE THE MAVEN JAR DOESN'T SEEM TO BE CORRECTLY PACAKAGED
# SPARK_ATLAS_URL=https://repository.cloudera.com/artifactory/libs-release-local/com/hortonworks/spark/spark-atlas-connector_2.11/0.1.0.7.2.7.5-1/spark-atlas-connector_2.11-0.1.0.7.2.7.5-1.jar

# maven coordinates: com.hortonworks.spark:spark-atlas-connector_2.11:0.1.0.7.2.7.5-1
# https://kb.databricks.com/libraries/replace-default-jar-new-jar.html
# https://askubuntu.com/questions/80065/i-want-to-copy-a-directory-from-one-place-to-another-via-the-command-line

# cp -a /dbfs/FileStore/jars/. /databricks/jars/
cp -v -a $INPUT_JARS $JAR_PATH
echo "finished copying jars to $JAR_PATH"

# wget and curl HANGING
# wget $SPARK_ATLAS_URL -P $JAR_PATH
# cd $JAR_PATH && { curl -O $SPARK_ATLAS_URL ; cd -; }

########################
# Spark Properties
########################
# https://mallikarjuna_g.gitbooks.io/spark/content/spark-properties.html
# databricks default is not $SPARK_HOME/conf
# databricks $SPARK_HOME is actually just the root of the VM, ie - /
# databricks has conf in subfolders under /databricks
SPARK_CONF=/databricks/spark/conf
# COMMON_CONF=/databricks/common/conf
# DRIVER_CONF=/databricks/driver/conf
# DATA_CONF=/databricks/data/conf

# find / -type d -name conf
# find /databricks -name spark-defaults.conf
# sudo apt-get -y install tree
# tree /databricks -I 'conda|conda*|python|python*'


ATLAS_PROPERTIES_FILE=atlas-application.properties
INPUT_ATLAS_PROPERTIES=/dbfs/databricks/scripts/$ATLAS_PROPERTIES_FILE
ATLAS_PROPERTIES_PATH=$SPARK_CONF/$ATLAS_PROPERTIES_FILE

# sudo rm -f $ATLAS_PROPERTIES_PATH
# cp /dbfs/databricks/scripts/atlas-application.properties /databricks/spark/conf/atlas-application.properties
cp -v $INPUT_ATLAS_PROPERTIES $SPARK_CONF
# cp -v $INPUT_ATLAS_PROPERTIES $COMMON_CONF
# cp -v $INPUT_ATLAS_PROPERTIES $DRIVER_CONF
# cp -v $INPUT_ATLAS_PROPERTIES $DATA_CONF
echo "finished copying atlas properties to conf folders"

# creating file with echo is HANGING
# echo 'atlas.client.type=rest' > $ATLAS_PROPERTIES_PATH
# echo 'atlas.rest.address=https://yaaf-purview.catalog.purview.azure.com' >> $ATLAS_PROPERTIES_PATH
# /api/atlas/v2

# cat $ATLAS_PROPERTIES_PATH
# sudo apt-get -y install tree
# tree /databricks -I 'conda|conda*|python|python*'

# ls $SPARK_CONF
# cat $SPARK_CONF/spark-env.sh
# cat $SPARK_CONF/spark-defaults.conf
# spark.driver.extraClassPath /fullpath/firs.jar:/fullpath/second.jar
# spark.executor.extraClassPath /fullpath/firs.jar:/fullpath/second.jar

# exit 1
