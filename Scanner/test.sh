#!/bin/bash

[ $# -ne 1 ] && echo "Supply Arguments" && exit 1

echo "Setting classpath . . ."
export CLASSPATH=.:463.jar
echo "classpath now reads: $CLASSPATH"

echo "Setting up test files for parsing . . ."
java Scanner < $1 > myTmp
java -jar scanner.jar < $1 > refTmp

diff myTmp refTmp > /tmp/"$1".diff

diffVAR=$?

if [ "$diffVAR" -ne 0 ]; then 
  less /tmp/"$1".diff
  rm /tmp/"$1".diff
else
  echo "No difference between your implementation"
  echo "and reference implementation output when running $1"
  rm /tmp/"$1".diff
fi
