#!/usr/bin/env bash

java -Xmx3000m -cp nlp-toolbelt.jar edu/cmu/ark/cli/Toolbelt "$@"
# 	--model models/linear-regression-ranker-reg500.ser.gz \
# 	--prefer-wh \
# 	--max-length 30 \
# 	--downweight-pro \
#     
# #   --verbose \
# #   --debug \
    
