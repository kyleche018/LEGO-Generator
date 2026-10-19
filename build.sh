#!/usr/bin/env bash
# Compile the LEGO Generator into out/
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -d out src/*.java
echo "Built OK -> out/"
