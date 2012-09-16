 
{
  Program to sort files of RECTYPE in ascending order of key.
  The records are assumed to be of type RECTYPE, where this type is defined
    as given below.  The type RECTYPE must include the following two fields:
      key : keytype;
      eoff : boolean;
    The type KEYTYPE may be any type for which direct comparisons are legal,
      such as packed array of char, integer, or real.  The type KEYTYPE must
      also be defined.  If KEYTYPE is a packed array of characters, the
      type KEYINDEXTYPE may be used as an index into this array.  The types
      and constants that must be defined are denoted by lines of asterisks
      '******'.
    The eoff field is used as an end-of-file mark, and tests for eof(file)
      are replaced by tests for file^.eoff.  The input file must contain
      at least one record.  All but the last record in the file must have
      its eoff field false, and the last record must have its eoff field
      true.  The remaining components of the last record are undefined.
  The sort routine uses replacement selection to generate temporary files
    containing runs obtained from the input file, and then uses k-way
    merging between  k  input and  k  output files to produce the sorted
    file which is copied into the output file.  In this program, the
    value of  k  is  numsort+1, where numsort is a constant defined in the
    program.  Because of system limitations, numsort generally cannot 
    exceed 4.
  The number of internal nodes in the replacement selection tree is governed
    by the constant maxselect.  The initial run lengths will have average
    size 2*(maxselect+1), so larger sizes of maxselect will generally decrease
    sort time.  
}
 
program sort;
 
const
{****************************************************************************}
  numkey=6; { number of characters in key }
{****************************************************************************}
  maxselect=31; { 0..maxselect leaf nodes in selection tree }
  numsort=4; { 0..numsort temporary files for merging }
 
 
type
{****************************************************************************}
  keyindextype = 1..numkey; { type used for index into key array }
  {**** keytype defines the type of the key field of the input records ****}
  keytype = packed array [keyindextype] of char;
  {**** rectype defines the record type of the input and output files ****}
  rectype=
    record
      key : keytype; {**** this field must be present ****}
      { other record components may appear in this record definition }
      eoff : boolean {**** this field must be present ****}
    end; { rectype }
{****************************************************************************}
  filetype=file of rectype;
  selectindextype=0..maxselect;
  switchtype=0..1;
  runtype=0..65535;
  fileindextype=
    record
      switch : switchtype;
      num : selectindextype;
      tmp : boolean
    end;
 
var
 
  infile,outfile : filetype;
  sortfile : array[0..1,0..numsort] of filetype; { work files for sort }
  sortfilename : array[0..1,0..numsort] of packed array[1..10] of char;
  inindex,outindex : fileindextype;
  pass : 0..maxint;
  runcount : runtype;
  infilename,outfilename : packed array[1..32] of char;
  filesize : integer; { size of the input file in blocks }
  reccount : 0..65535; { number of records in input file }
 
procedure switchfiles(nsort : selectindextype);
  { switches the roles of the temporary input & output files for next merge }
  var
    t : switchtype;
    n : selectindextype;
  begin
    if nsort<1 then nsort:=1;
    t := outindex.switch;
    outindex.switch := inindex.switch;
    inindex.switch := t;
    for n:=0 to nsort do
      begin
        with outindex do
          if tmp then rewrite(sortfile[switch,n],sortfilename[switch,n]);
        with inindex do
          if tmp then
            begin
              close(sortfile[switch,n]);
              reset(sortfile[switch,n],sortfilename[switch,n])
            end
      end;
    inindex.num := 0;
    outindex.num := numsort { incremented before first write }
  end; { switchfiles }
 
procedure select(numselect : selectindextype; var currun : runtype);
  {
    Perform replacement selection merge from input files to output files.
    The parameter numselect is the number of nodes in the selection tree
    to be used; in the first pass, the value of numselect determines the
    average length of the initial runs.  In subsequent passes, numselect
    determines the number of temporary input and output files to be used.
    The global variables inindex and outindex control the source and
    destination of the sort and merge runs:  if inindex.tmp is false,
    the input is taken from the file infile, otherwise the input is taken
    from the scratch files sortfile[infile.switch,*]; similarly if
    outindex.tmp is false, merged output is written to the file outfile, 
    otherwise it is written to the scratch files sortfile[outfile.switch,*].
  }
 
  type
    selectitemtype=
      record
        rec : rectype; { record type to be sorted, including key }
        loser : selectindextype; { internal node pointer to loser record }
        run : runtype; { internal run number of record pointed to by loser }
        extptr,intptr : selectindextype { pointers to nodes higher in tree }
      end;
   
  var
    selectarray : array [selectindextype] of selectitemtype;
    maxrun : runtype; { run no. of last run output }
    lastkey : keytype; { value of last key output }
    winptr : selectindextype; { pointer to current winner }
    winrun : runtype; { run no. of current winner }
    treeindex : selectindextype; { used to move up the selection tree }
    done : boolean; { controls main select loop }
    moveup : boolean; { controls ascent of tree }
   
  procedure selectinit;
    var
      t : selectindextype;
    begin
      reccount := 0; { number of records }
      maxrun := 0;
      currun := 0;
      winptr := 0;
      winrun := 0;
      for t:=0 to numselect do
        with selectarray[t] do
          begin
            loser:=t;
            run:=0;
            extptr:=(numselect+1+t) div 2;
            intptr:=t div 2
          end
    end; { selectinit }
 
procedure getfile;
  begin
    with selectarray[winptr],inindex do
      if tmp
        then read(sortfile[switch,winptr],rec)
        else read(infile,rec)
  end; { getfile }
 
procedure putfile;
  begin
    with selectarray[winptr] do
      begin
        lastkey:=rec.key;
        with outindex do
          if tmp
            then write(sortfile[switch,num],rec)
            else write(outfile,rec)
      end; { with selectarray }
    reccount := reccount+1
  end; { putfile }
 
function endfile : boolean;
  begin
    with inindex do
      if tmp
        then endfile:=sortfile[switch,winptr]^.eoff
        else endfile:=infile^.eoff
  end;  { endfile }
 
  procedure writeeofrecs;
    { write end-of-file records at the ends of the scratch files }
    var
      n : selectindextype;
      numsel : selectindextype;
    begin
      numsel:=numselect;
      if numsel>numsort then numsel:=numsort;
      with outindex do
        if tmp
          then
            for n:=0 to numsel do
              begin
                sortfile[switch,n]^.eoff:=true;
                put(sortfile[switch,n])
              end
          else
            begin
              outfile^.eoff:=true;
              put(outfile)
            end
    end; { writeeofrecs }
 
  procedure exchangeloser;
    { exchange loser[treeindex] and winptr }
    var
      tempwin : selectindextype;
    begin
      with selectarray[treeindex] do
        begin
          tempwin := winptr;
          winptr := loser;
          loser := tempwin
        end
    end; { exchangeloser }
 
  procedure exchangerun;
    { exchange current run no. and winrun }
    var
      temprun : runtype;
    begin
      with selectarray[treeindex] do
        begin
          temprun := winrun;
          winrun := run;
          run := temprun
        end
    end; { exchangerunno }
 
  begin { select }
    if numselect<1 then numselect:=1;
    selectinit;
    done := false;
    while not done do
      begin
        while winrun=currun do
          begin
            if winrun<>0 then putfile;
            if endfile
              then winrun := maxint { make permanently inaccessible }
              else
                begin
                  getfile;
                  if (winrun=0) or (selectarray[winptr].rec.key<lastkey) then
                    begin
                      winrun := winrun+1;
                      if winrun > maxrun then maxrun := winrun
                    end
                end;
            treeindex := selectarray[winptr].extptr; { chase up tree }
            moveup := true;
            while moveup do
              with selectarray[treeindex] do
                begin
                  if run < winrun
                    then
                      begin
                        exchangeloser;
                        exchangerun
                      end
                    else if (run=winrun)
                         and (selectarray[loser].rec.key
                              < selectarray[winptr].rec.key)
                      then exchangeloser;
                  if treeindex=1
                    then moveup := false
                    else treeindex := intptr
                end
          end;
        if winrun>maxrun
          then
            begin
              writeeofrecs;
              done:=true
            end
          else
            begin
              currun := winrun;
              with outindex do num := (num+1) mod (numsort+1)
              { switch to next output scratch file }
            end
      end { while not done }
  end; { select }
 
procedure sortexit;
  var
    k : switchtype;
    l : selectindextype;
  begin
    for k := 0 to 1 do
      for l := 0 to numsort do
        delete(sortfile[k,l])
  end; { sortexit }
 
procedure sortinit;
  var
    k : switchtype;
    l : selectindextype;
  begin
    for k:= 0 to 1 do
      for l := 0 to numsort do
        begin
          sortfilename[k,l]:='SORT  .TMP';
          sortfilename[k,l][5]:=chr(k+ord('0'));
          sortfilename[k,l][6]:=chr(l+ord('0'));
          rewrite(sortfile[k,l],sortfilename[k,l])
        end;
    with inindex do
      begin
        switch := 1;
        tmp := false { initial input from infile }
      end;
    with outindex do
      begin
        switch := 0;
        num := numsort;
        tmp := true { output goes to temp sort files }
      end;
  end; { sortinit }
 
procedure printstats;
  begin
    write(runcount:4,' runs, ');
    write(reccount:6,' records');
    writeln
  end; { printstats }
 
begin { main }
  write('Input file: ');
  readln(infilename);
  reset(infile,infilename,,filesize);
  if filesize<0 then writeln('  File is non-existent or protected . . .')
  else if filesize=0 then writeln('  File is empty . . .')
  else
    begin
      write('Output file: ');
      readln(outfilename);
      writeln;
      sortinit; { initialize scratch files }
      pass := 0;
{
      write(' Run generation . . . ');
}
      select(maxselect,runcount); { generate first runs to temp sort files }
{
      printstats;
}
      inindex.tmp := true; { subsequent input is from temp sort files }
      while runcount>numsort+1 do
        begin
          pass := pass+1;
{
          write(' Merge pass ',pass:3,' . . . ');
}
          switchfiles(numsort); { switch roles of input & output sort files }
          select(numsort,runcount); { merge }
{
          printstats;
}
        end;
{
      write(' Final merge    . . . ');
}
      switchfiles(runcount-1);
      rewrite(outfile,outfilename);
      outindex.tmp := false; { final output goes to outfile }
      select(runcount-1,runcount); { final merge }
      sortexit; { delete temporary sort files }
{
      writeln(reccount:7,' records sorted')
}
    end
end.

