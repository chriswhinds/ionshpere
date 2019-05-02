#!/usr/bin/env bash
# ------------------------------------------------------------------------------------
# startstaticsvc.sh - Start/Stop Script for the Static Service
## Environment Variable Prequisites
#
#   SERVICE_HOME (Optional) May point at your Servers installation directory.
#                 If not present, the current working directory is assumed.
#
#   JAVA_HOME     Optional, point to a differnet version of java if not the system default for the operating systems installation.
#   LOGGING_CONFIG
#
#
#Created: start.sh,v 1.0 2017/02/08 20:01:21 chinds
# -----------------------------------------------------------------------------
SERVICE_HOME=`cd ..;pwd`
CONFIG_LOC=${SERVICE_HOME}/conf
LOGGING_CONFIG="-Dlog4j.configurationFile=${CONFIG_LOC}/staticsvclog4j2config.xml"

if [ "$1" != "DEBUG" ]; then
    ./start.sh com.droitfintech.staticservice.StaticDataReferenceDataService staticsvc $LOGGING_CONFIG
else
    ./start.sh com.droitfintech.staticservice.StaticDataReferenceDataService staticsvc $LOGGING_CONFIG DEBUG 7777
fi
