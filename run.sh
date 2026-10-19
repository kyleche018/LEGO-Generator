#!/usr/bin/env bash
# Run the LEGO Generator GUI (build first with ./build.sh)
cd "$(dirname "$0")"
java -cp out Main "$@"
