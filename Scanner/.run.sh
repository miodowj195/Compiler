#!/bin/bash

[[ ! -d build ]] && 
	echo "Source files not compiled... please run build.sh before continuing."

export CLASSPATH=.:463.jar

cd build/

java Scanner

