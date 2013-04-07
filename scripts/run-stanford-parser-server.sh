#/usr/bin/env bash

java -Xmx3000m -cp nlp-toolbelt.jar edu/cmu/ark/cli/Toolbelt stanford-parser-server "$@"



