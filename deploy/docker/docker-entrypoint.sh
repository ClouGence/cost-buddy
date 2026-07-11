#!/usr/bin/env bash
set -euo pipefail

APP_HOME=/home/costbuddy/app
LOG_DIR=${APP_HOME}/logs

mkdir -p "${LOG_DIR}"

if [[ "$(id -u)" = "0" ]]; then
  chown -R costbuddy:costbuddy "${LOG_DIR}"
  exec runuser -u costbuddy -- java -Duser.home=/home/costbuddy ${JAVA_OPTS:-} -jar "${APP_HOME}/cost-buddy.jar"
fi

exec java -Duser.home=/home/costbuddy ${JAVA_OPTS:-} -jar "${APP_HOME}/cost-buddy.jar"
