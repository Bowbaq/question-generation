# requires python, java

# Before running anything, the first step is to launch the Stanford parser
#  server.  To do that, type:
zsh ./scripts/run-stanford-parser-server.sh &

# Next, launch the super sense tagger server.  To do so, type:
zsh ./scripts/runSSTServer.sh &

# Now, type this to generate questions:
./ask scripts/sample.txt 15
# where n is the number of questions

#To answer questions, type:
./answer scripts/sample.txt scripts/questions.txt

