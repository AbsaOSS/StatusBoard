#!/bin/sh
if [ -f "/opt/config/config.conf" ]; then
    echo "Running with custom config"
    exec java -Dconfig.file=/opt/config/config.conf -jar status-board-assembly.jar
else
    echo "Running with default config"
    exec java -jar status-board-assembly.jar
fi
