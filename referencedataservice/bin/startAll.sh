#!/usr/bin/env bash
# ------------------------------------------------------------------------------------
# startAll.sh - Start/Stop Script for the platform Service Adapter Container
## Environment Variable Prequisites
#  Start All Services
#
#Created: start.sh,v 1.0 2017/02/08 20:01:21 chinds
# -----------------------------------------------------------------------------
./startauditsvc.sh
./startpartysvc.sh
./startstaticsvc.sh
./startdefaultingsvc.sh
