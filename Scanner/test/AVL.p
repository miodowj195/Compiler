{ ACS 340               AVL Trees                              John D. Cotter }

program avl(input, output);

{ This program will build an AVL tree from the data given in 'MasterFile3'    }
{ This file is a file of records containing three fields:                     }
{   		key : packed array [1..6] of char;                            }
{    		quantity : integer;                                           }
{           	description : packed arrray [1..15]                           }
{									      }
{ The AVL tree is then searched for certain records (the ones given in the    }
{ file SearchKeys).  Each node visited during a particular search is recorded }
{ until the key is found or determined that it is not in the AVL tree.        }

type
  balancefactor = (LH, EH, RH);
  pntr = ^node;
  number = packed array [1..6] of char;
  description = packed array [1..15] of char;

  dataRecord = record
                 key : number;
                 quantity : integer;
                 whatisit : description
               end;

  node = record
           info : dataRecord;
           left,
           right : pntr;
           bf : balancefactor
         end;

  list = file of dataRecord;

var
  inFile : list;
  searchFile, outFile : text;
  item : dataRecord;
  root, newnode : pntr;
  taller,found : Boolean;


{*************functions and procedures*****************************************}

procedure Error;

{ This procedure will be called in the event that something goes amuck        }

begin
  writeln(outFile,'Error in building AVL tree! ');
end;

procedure RotateLeft (var p: pntr);

{ This procedure will rebalance the tree by rotating it left.                 }

var
   temp: pntr;

begin
   if p = nil then
      Error
   else if p^.right = nil then
      Error
   else
      begin
	 temp := p^.right;
	 p^.right := temp^.left;
	 temp^.left := p;
	 p := temp
      end
end;

{**************************************************************************}

procedure RotateRight (var p: pntr);

{ This procedure will rebalance the tree by rotating it right.                 }

var
   temp: pntr;

begin
   if p = nil then
      Error
   else if p^.left = nil then
      Error
   else
      begin
	 temp := p^.left;
	 p^.left := temp^.right;
	 temp^.right := p;
	 p := temp
      end
end;

{**************************************************************************}

procedure Insert (var root: pntr; newnode: pntr; var taller: Boolean);

{ This procedure inserts a node into the AVL tree (recursively!). It then   }
{ checks the balance of the tree and decides if it must be rebalanced.      }
{ Because it recursively calls itself it auotmatically rebalances the tree  }
{ pivoting around the node farthest down the tree that goes critical.       }

var
   tallersubtree: Boolean;



   procedure LeftBalance;

{ This procedure determines which rotations must be done to accomplish the  }
{ rebalancing of the tree.  It also takes care of modifying the balnce-     }
{ factors when necessary.                                                   }
{     Procedure is called when AVL tree is right heavy.                     }

   var
      x,
      w: pntr;

   begin
      x := root^.left;
      case x^.bf of
	 LH: begin
		root^.bf := EH;
		x^.bf := EH;
		RotateRight (root);
		taller := false
	     end;
	 EH: Error;
	 RH: begin
		w := x^.right;
		case w^.bf of
		   EH: begin
			  root^.bf := EH;
			  x^.bf := EH
		       end;
		   RH: begin
			  root^.bf := EH;
			  x^.bf := LH
		       end;
		   LH: begin
			  root^.bf :=RH;
			  x^.bf := EH
		       end;
	    
		end;
		w^.bf := EH;
		RotateLeft (x);
		root^.left := x;
		RotateRight (root);
		taller := false
	     end
      end
   end;

      {********************************************}

   procedure RightBalance;

{ This procedure determines which rotations must be done to accomplish the  }
{ rebalancing of the tree.  It also takes care of modifying the balnce-     }
{ factors when necessary.                                                   }
{     Procedure is called when AVL tree is left heavy.                      }
   var
      x,
      w: pntr;

   begin
      x := root^.right;
      case x^.bf of
	 RH: begin
		root^.bf := EH;
		x^.bf := EH;
		RotateLeft (root);
		taller := false
	     end;
	 EH: Error;
	 LH: begin
		w := x^.left;
		case w^.bf of
		   EH: begin
			  root^.bf := EH;
			  x^.bf := EH
		       end;
		   LH: begin
			  root^.bf := EH;
			  x^.bf := RH
		       end;
		   RH: begin
			  root^.bf :=LH;
			  x^.bf := EH
		       end;
	    
		end;
		w^.bf := EH;
		RotateRight (x);
		root^.right := x;
		RotateLeft (root);
		taller := false
	     end
      end
   end;

      {********************************************}


begin
   if root = nil then
      begin
	 root := newnode;
	 root^.left := nil;
	 root^.right := nil;
	 root^.bf := EH;
	 taller := true
      end
   else
      with root^ do
	 if newnode^.info.key = info.key then
	    Error
	 else if newnode^.info.key < info.key then
	    begin
	       Insert (left, newnode, tallersubtree);
	       if tallersubtree then
		  case bf of
		     LH: LeftBalance;
		     EH: begin
			    bf := LH;
			    taller := true
			 end;
		     RH: begin
			    bf := EH;
			    taller := false
			 end
		  end
	       else
		  taller := false
	    end
	 else
	    begin
	       Insert (right, newnode, tallersubtree);
	       if tallersubtree then
		  case bf of
		     LH: begin
			    bf := EH;
			    taller := false
			 end;
		     EH: begin
			    bf := RH;
			    taller := true
			 end;
		     RH: RightBalance
		  end
	       else
		  taller := false
	    end
end;


procedure BuildAVLTree(var root,newnode:pntr;var taller: Boolean; 
                       var inFile:list);

{ This procedure builds the AVL tree by first reading a record, then inserting }
{ into the tree (using 'Insert').                                              }

begin
  root := nil;
  reset(inFile,'/usr1/student/ACS340/MasterFile3');
  while not eof(inFile) do
    begin
      new(newnode);
      read (inFile, item);
      newnode^.info := item;
      Insert(root,newnode,taller);
    end;
end;


procedure Search(root:pntr; searchkey:number; found:Boolean);

{ This procedure searches said AVL tree for given search key by comparing  }
{ against the value of the current node^.info.key and then branching       }
{ accordingly.                                                             }


begin
   found := false;
   while not found do
     begin
       if root = nil then
       begin
         writeln(outFile,'Record being searched for is not in AVL tree.');
	 writeln(outFile);
	 found := true
       end
       else if root^.info.key = searchkey then
	 begin
           found := true;
	   writeln(outFile,'final node visited = ',root^.info.key);
	   writeln(outFile);
         end;
       if not found then
	 begin
           writeln(outFile,'visited node^.key = ',root^.info.key);
           if root^.info.key > searchkey then
	     root := root^.left
           else
	     root := root^.right
	 end {if}
     end {while}
end;


procedure SearchTree(var root:pntr; var found:Boolean; var searchFile:text);

{ This procedure reads in a searchKey from the file 'SearchKeys' and then  }
{ calls the procedure 'Search' passinf it the search key.                  }

var 
  searchkey : number;
  ch : char;	
  i : integer;

begin
  reset(searchFile,'/usr1/student/ACS340/SearchKeys');
  rewrite(outFile,'search.out');
  writeln(outFile);
  writeln(outFile, 'ACS 340                         AVL Trees                   John D. Cotter'); 
  writeln(outFile,'--------------------------------------------------------------------------');
  writeln(outFile);
  i := 0;
  while not eof(searchFile) do
    begin
      while not eoln(searchFile) do
        begin
          read(searchFile, ch);
          i := i + 1;
          searchkey[i] := ch;
        end;
      i := 0
      readln(searchFile);
      writeln(outFile,'searchkey = ',searchkey);
      Search(root,searchkey, found);
    end
end;


{********Main Program*****************************************}

begin
  BuildAVLTree(root,newnode,taller,inFile);
  SearchTree(root,found,searchFile) 
end.
