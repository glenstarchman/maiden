
export MAIDEN_MODE=test
sbt "migrate rebuild"
#sbt "run" > test.log 2>&1 &
sbt "test"
