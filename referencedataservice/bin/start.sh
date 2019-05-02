#!/usr/bin/env bash
# -xv ADD TRACE
# ------------------------------------------------------------------------------------
# start.sh - Start/Stop Script for the platform Service Adapter Container
## Environment Variable Prequisites
#
#   SERVICE_HOME (Optional) May point at your Servers installation directory.
#                 If not present, the current working directory is assumed.
#
#   JAVA_HOME     Optional, point to a differnet version of java if not the system default for the operating systems installation.
#   LOGGING_CONFIG
#   SERVICE_CONFIG
#
#Created: start.sh,v 1.0 2017/02/08 20:01:21 chinds
# -----------------------------------------------------------------------------
#SET UP SERVICE HOME
SERVICE_HOME=`cd ..;pwd`

echo "Service Running from "$SERVICE_HOME
echo "Sevice Class Name config parm pointer-> $1 "
echo "log prefix-> $2 "
echo "Logging config parm pointer-> $3 "
echo "DEBUG options switch-> $4 "
echo "DEBUG PORT-> $5 "


#SET runtime vars
SERVICE_CLASS_NAME="$1"
LOG_PREFIX="$2"
LOGGING_CONFIG="$3"
DEBUG_MODE="$4"
DEBUG_PORT="$5"


CONFIG_LOC=${SERVICE_HOME}/conf


#DEBUG OPTIONS
JAVA_DEBUG_OPTIONS="  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${DEBUG_PORT} "


JAVA_OPTIONS=" -Xmx1g  "

SERVICE_CONFIG="-Dservice.configurationFile=${CONFIG_LOC}/service-config.yml"
SERVICE_ROOT_LOCATION="-Dservice.home=${SERVICE_HOME}"
STDOUT_LOG="${SERVICE_HOME}/logs/${LOG_PREFIX}-console.log"
STDERR_LOG="${SERVICE_HOME}/logs/${LOG_PREFIX}-errors.log"
JAVA_CMD="java "


if [ "$4" == "DEBUG" ]; then
JAVA_OPTIONS+=$JAVA_DEBUG_OPTIONS
fi

#RUN the SERVICE

#START THE RUN
# ----- Execute The Requested Command -----------------------------------------
echo "====Starting Batch Service Using ============================="
echo "SERVICE_HOME= $SERVICE_HOME"
echo "SERVICE_ROOT_LOCATION= $SERVICE_ROOT_LOCATION"
echo "JDK OPTIONS : $JAVA_OPTIONS"
echo "Service Class Name : $SERVICE_CLASS_NAME"
echo "LOGGING CONFIG LOC: $LOGGING_CONFIG"
echo "SERVICES CONFIG LOC: $SERVICE_CONFIG"
echo "STDOUT LOG: $STDOUT_LOG"
echo "STDERR LOG: $STDERR_LOG"
echo "JDK CMD : $JAVA_CMD"
echo "========================================================"

exec $JAVA_CMD $JAVA_OPTIONS \
               $LOGGING_CONFIG \
               $SERVICE_ROOT_LOCATION \
               $SERVICE_CONFIG \
                 -jar droitfintech-referencedataservice.jar $SERVICE_CLASS_NAME 1>$STDOUT_LOG 2>$STDERR_LOG &


# === Dinamically generate the shutdown script , after every restart overlay the file.
SERVICE_SHUTDOWN_COMMAND="${LOG_PREFIX}-shutdown.sh"
echo "kill -15 "$! >$SERVICE_SHUTDOWN_COMMAND
chmod 777 $SERVICE_SHUTDOWN_COMMAND
