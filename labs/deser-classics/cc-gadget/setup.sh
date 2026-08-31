#!/bin/bash
# Fetches the jars needed to compile/run the CC6 gadget generator and Shiro
# cookie encryptor locally (these aren't in the repo -- see .gitignore).
set -e
cd "$(dirname "$0")"
curl -sL -o commons-collections-3.2.1.jar \
  https://repo1.maven.org/maven2/commons-collections/commons-collections/3.2.1/commons-collections-3.2.1.jar
echo "done: commons-collections-3.2.1.jar"
