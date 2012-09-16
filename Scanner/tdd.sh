Download
#!/bin/bash

[ $# -ne 1 ] && echo "Supply single file or dir of files to test." && exit 1

# Choose file mode if arg1 isnt a dir
mode="file"
[ -d $1 ] && mode="dir"


# $1 is now the first arg to the function
run(){
  java Scanner < "test/"$1 > myTmp
  java -jar scanner.jar < "test/"$1 > refTmp

  tmp=$(basename $1)

  diff myTmp refTmp > /tmp/$tmp.diff

  # Check diff's return value: 0 if there is NO DIFF, and 1 if there IS a diff
  diffVAR=$?

  # Echo diff result
  if [ "$diffVAR" -ne 0 ]; then 
    echo "File: $1, THERE IS A DIFFERENCE."
    cat /tmp/$tmp.diff
    rm /tmp/$tmp.diff
    exit 1 # exit on the first fail
  else
    echo "File: $1, NO DIFFERENCE"
    rm /tmp/$tmp.diff
  fi
}


echo "Setting classpath . . ." 
export CLASSPATH=.:463.jar
echo "classpath now reads: $CLASSPATH"

echo "Running Tests . . ."
# if mode is DIR loop over contents
if [ "$mode" == "dir" ]; then
  for f in $(ls $1); do
    run $f
  done
else # else, just run on the file
  run $1
fi
