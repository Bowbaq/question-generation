#/usr/bin/env zsh

java -Xmx3000m -cp nlp-toolbelt.jar edu/cmu/ark/cli/Toolbelt stanford-parser-server "$@"



