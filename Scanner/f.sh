#!/bin/bash

for k in $(ls ./test); do 
  
  java Scanner < "$k" > /tmp/myTMP
  java -jar scanner.jar < "$k" > /tmp/refTMP

  diff /tmp/myTMP /tmp/refTMP > "$k".diff

  diffVAR=$?

  if[ "diffVAR" -ne 0 ]; then
      
  ./test.sh $k; done
