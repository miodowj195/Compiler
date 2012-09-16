#!/bin/bash

if [[ ! -d src  || ! -f src/463.jar ]]; then
  echo "NO SRC OR 463.jar"
  exit 1
fi

[ -d build/ ] && rm -r build/
[ ! -d javadoc/ ] && mkdir javadoc 

mkdir build/ && cp src/463.jar build &&
  javac -classpath src/463.jar -d build/ src/*.java

rm -r javadoc/ && mkdir javadoc &&
  javadoc -classpath src/463.jar -d javadoc/ src/*.java

