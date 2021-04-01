# spark-atlas-connector for databricks & purview
attempting to automatically capture databricks lineage in purview.  
note the __limitations__ of SAC in the main [readme](../README.md).

## patch_spark_model (one-time run)
[reference code](https://github.com/wjohnson/pyapacheatlas/blob/master/samples/new_lineage_processes_typedefs.py)  
purview's VM is not accessible. we will need to manually create the spark model via REST API (`pyapacheatlas`).  
use `purview_spark.py`. there is an extra copy of `1100-spark_model.json` from the `patch` folder which will be parsed by `purview_spark.py`.
```
python purview_spark.py -h      
usage: purview_spark.py [-h] -t TENANT_ID -c CLIENT_ID -s CLIENT_SECRET
                        [-n PURVIEW_NAME] [--spark_model SPARK_MODEL]

Create spark model in Azure Purview

optional arguments:
  -h, --help            show this help message and exit
  -t TENANT_ID, --tenant_id TENANT_ID
                        AAD Tenant ID
  -c CLIENT_ID, --client_id CLIENT_ID
                        SPN Client/Application ID
  -s CLIENT_SECRET, --client_secret CLIENT_SECRET
                        SPN Secret
  -n PURVIEW_NAME, --purview_name PURVIEW_NAME
                        default is yaaf-purview
  --spark_model SPARK_MODEL
                        default is 1100-spark_model.json
```

## jar dependencies
__CONFIRMED:__ spark-atlas-connector-assembly jar works on local as expected  
If you want to use the thin jar then be sure to have the following:
#### atlas version 2.1.0
- atlas-intg: [maven](https://mvnrepository.com/artifact/org.apache.atlas/atlas-intg/2.1.0)
- atlas-client-common: [maven](https://mvnrepository.com/artifact/org.apache.atlas/atlas-client-common/2.1.0)
#### jersey version 1.19
- jersey-client: [maven](https://mvnrepository.com/artifact/com.sun.jersey/jersey-client/1.19)
- jersey-core: [maven](https://mvnrepository.com/artifact/com.sun.jersey/jersey-core/1.19)
- jersey-multipart: [maven](https://mvnrepository.com/artifact/com.sun.jersey.contribs/jersey-multipart/1.19)
#### jackson version 2.9.9
- jackson-jaxrs-json-provider: [maven](https://mvnrepository.com/artifact/com.fasterxml.jackson.jaxrs/jackson-jaxrs-json-provider/2.9.9)
- jackson-jaxrs-base: [maven](https://mvnrepository.com/artifact/com.fasterxml.jackson.jaxrs/jackson-jaxrs-base/2.9.9)
#### org.json > json > 20210307
- json: [maven](https://mvnrepository.com/artifact/org.json/json/20210307)

## scripts
[Cluster-scoped init scripts](https://docs.databricks.com/clusters/init-scripts.html#cluster-scoped-init-script-locations) need to be uploaded to [dbfs root](https://docs.databricks.com/data/databricks-file-system.html#dbfs-root).
#### databricks cli
```bash
# cleanup
databricks fs rm -r dbfs:/databricks/scripts
# copy and overwrite scripts folder to dbfs
databricks fs cp --recursive --overwrite ./scripts dbfs:/databricks/scripts
# verify copy
databricks fs ls dbfs:/databricks/scripts
databricks fs ls dbfs:/databricks/scripts/jars
databricks fs cat dbfs:/databricks/scripts/spark-atlas.sh
# check logfile after cluster start. logfile is from spark-atlas.sh
databricks fs cat dbfs:/databricks/scripts/logfile.txt
```
#### spark-atlas.sh template
__NOTE:__ if you are getting `init script exit status non-zero` when starting the databricks cluster with init script, it is very likely due to [lack of Unix-style line endings](https://github.com/dotnet/spark/issues/183).  
Ensure you enable [cluster log delivery](https://docs.databricks.com/clusters/configure.html#cluster-log-delivery) in advanced options for [easier debugging of init scripts](https://docs.databricks.com/clusters/init-scripts.html#init-script-logs). The stderr.log will show `$'\r': command not found`.  
If using VSCode, [change the line endings for the file](https://stackoverflow.com/questions/39525417/visual-studio-code-how-to-show-line-endings) from CRLF to LF. This can also be done for the workspace via `Files > Preferences > Settings > Files: Eol > \n`.
```bash
#!/bin/sh

SCRIPT_ROOT=/dbfs/databricks/scripts
LOG_PATH=$SCRIPT_ROOT/logfile.txt

# redirect stdout/stderr to a file
exec &> $LOG_PATH

# echo "hello world"
# echo "SPARK_CONF_DIR: $SPARK_CONF_DIR"

########################
# JARS
########################
INPUT_JARS=$SCRIPT_ROOT/jars/*
JAR_PATH=/databricks/jars
# NOTE THE MAVEN JAR DOESN'T SEEM TO BE CORRECTLY PACAKAGED
# SPARK_ATLAS_URL=https://repository.cloudera.com/artifactory/libs-release-local/com/hortonworks/spark/spark-atlas-connector_2.11/0.1.0.7.2.7.5-1/spark-atlas-connector_2.11-0.1.0.7.2.7.5-1.jar

# maven coordinates: com.hortonworks.spark:spark-atlas-connector_2.11:0.1.0.7.2.7.5-1
# https://kb.databricks.com/libraries/replace-default-jar-new-jar.html
# https://askubuntu.com/questions/80065/i-want-to-copy-a-directory-from-one-place-to-another-via-the-command-line

# cp -a /dbfs/FileStore/jars/. /databricks/jars/
# cp -v $INPUT_JARS $JAR_PATH
# ls $INPUT_JARS
echo "=== finished copying jars to $JAR_PATH"

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
SPARK_CONF=/databricks/spark/conf/
# COMMON_CONF=/databricks/common/conf
# DRIVER_CONF=/databricks/driver/conf
# DATA_CONF=/databricks/data/conf

# find / -type d -name conf
# find /databricks -name spark-defaults.conf
# sudo apt-get -y install tree
# tree /databricks -I 'conda|conda*|python|python*'


ATLAS_PROPERTIES_FILE=atlas-application.properties
INPUT_ATLAS_PROPERTIES=$SCRIPT_ROOT/$ATLAS_PROPERTIES_FILE
# ATLAS_PROPERTIES_PATH=$SPARK_CONF/$ATLAS_PROPERTIES_FILE

# sudo rm -f $ATLAS_PROPERTIES_PATH
# cp /dbfs/databricks/scripts/atlas-application.properties /databricks/spark/conf/atlas-application.properties
# cp -v $INPUT_ATLAS_PROPERTIES $SPARK_CONF
# cp -v $INPUT_ATLAS_PROPERTIES $COMMON_CONF
# cp -v $INPUT_ATLAS_PROPERTIES $DRIVER_CONF
# cp -v $INPUT_ATLAS_PROPERTIES $DATA_CONF
# ls $SCRIPT_ROOT
echo "=== finished copying atlas properties to conf folders"

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

```

## cluster configuration - advanced options
reference material for databricks cluster configuration
## init script
point to `spark-atlas.sh` that you upload to `dbfs:/databricks/scripts`
### spark config
```
spark.driver.extraJavaOptions -Datlas.conf=/databricks/spark/conf
spark.executor.extraJavaOptions -Datlas.conf=/databricks/spark/conf
spark.extraListeners com.sparkview.spark.atlas.SparkAtlasEventTracker
spark.sql.queryExecutionListeners com.sparkview.spark.atlas.SparkAtlasEventTracker
spark.sql.streaming.streamingQueryListeners com.sparkview.spark.atlas.SparkAtlasStreamingQueryEventTracker
spark.driver.extraClassPath /databricks/jars/spark-atlas-connector-assembly-0.1.1.jar
spark.executor.extraClassPath /databricks/jars/spark-atlas-connector-assembly-0.1.1.jar
```
### environment variables
```
TENANT_ID=abc
CLIENT_ID=id
CLIENT_SECRET=secret
ATLAS_CLIENT_TYPE=rest
ATLAS_REST_ADDRESS=https://test-name.catalog.purview.azure.com
```
### databricks cluster node tree
refer to ./databricks-tree.txt for the tree of the /databricks folder on the cluster nodes.

conf folders on the cluster nodes:
```
/usr/local/lib/R/site-library/sparklyr/conf
/databricks/spark/conf
/databricks/spark/python/test_coverage/conf
/databricks/common/conf
/databricks/driver/conf
/databricks/hive/conf
/databricks/data/conf
find: â€˜/sys/kernel/security/imaâ€™: Permission denied
find: â€˜/sys/kernel/security/apparmorâ€™: Permission denied
find: â€˜/sys/kernel/debugâ€™: Permission denied
find: â€˜/sys/fs/pstoreâ€™: Permission denied
find: â€˜/sys/fs/fuse/connections/44â€™: Permission denied
/proc/sys/net/ipv4/conf
/proc/sys/net/ipv6/conf
find: â€˜/proc/tty/driverâ€™: Permission denied
/var/lib/apache2/conf
/var/lib/ganglia-web/conf
```

## local spark dev (windows)
reference material for running local spark on windows machine. linux would be very similar.
### required installations
- python 3.7.x
- java jdk 1.8 (java 8) (install at root of C: drive)
- spark 2.4.5 with hadoop 7 (install at root of C: drive)
  - spark 3 install is same. choose the latest hadoop version. adjust environment variables accordingly.
### environment variables in windows
```bash
# python path might not be needed
PYTHONPATH=C:\Program Files\Python37\;C:\Program Files\Python37\Scripts\;%SPARK_HOME%\python\lib\py4j-0.10.7-src.zip

JAVA_HOME=C:\Java\jdk1.8.0_251
SPARK_HOME=C:\spark-2.4.5-bin-hadoop2.7
HADOOP_HOME=C:\spark-2.4.5-bin-hadoop2.7
# add to path
Path=C:\Program Files\Python37\Scripts\;C:\Program Files\Python37\;%JAVA_HOME%\bin;%SPARK_HOME%\bin
```
### pyspark or spark-shell
spark-shell is great for troubleshooting the java/scala classes.
```python
# if spark-shell, use val df = ...
df = spark.read.option("inferSchema", "true").option("header", "true").csv("country_vaccinations.csv")
df.printSchema()
df.write.mode("overwrite").partitionBy("country").parquet("country_vaccinations_output")
```
