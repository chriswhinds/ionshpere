#!/usr/bin/env bash
# ------------------------------------------------------------------------------------
# shutdownAll.sh - Stop Script for the platform Service Adapter Container
## Environment Variable Prequisites
#  stop All Services
#
#Created: start.sh,v 1.0 2017/02/08 20:01:21 chinds
# -----------------------------------------------------------------------------
./auditsvc-shutdown.sh
./partysvc-shutdown.sh
./defaultingsvc-shutdown.sh
./staticsvc-shutdown.sh
