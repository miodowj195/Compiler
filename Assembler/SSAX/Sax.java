import cis463.util.*;
import cis463.fsm.*;
import java.util.*;
import java.io.*;
import gnu.getopt.Getopt;

public class Sax {

  public Map<String, ArrayList<Integer>> labelToLineNo = null;
  public boolean dwBool = false; //check to see entry into sax_DWS
  public LineIO lio;
  public FileReader rdr;
  public boolean op0 = false; //tells you if you have an op on the line
  public boolean operand = false; //tells you if you have an operand 
  public BufferedReader BR;
  public PrintStream lst; // the listing file
  public StreamReader<Token> tokio;
  public Lazy<Token> lztok;
  public int pass;
  public boolean legacy = false;  // generate legacy (non-CALLS) code?
  public Map<String, Value> symtab;
  public List<Integer> relocationDictionary;
  public String entryID; // the named entry point
  public List<String> publicList;
  public List<String> externList;
  public String list;    // name of list file
  public String out;     // name of object module output file
  public String prog;    // name of source file
  public Sax sax;        // it's all about me, me, me!!
  public int errorCount; // count of syntax errors (don't do pass 2 if >0)
  public List<String> errorList;   // current line error messages
  public String MRNL = null; //the most recently seen non local label

  public Sax (String list,
      String out,
      String prog,
      boolean legacy) {
    this.list = list;
    this.out = out;
    this.prog = prog;
    this.legacy = legacy;
    this.labelToLineNo = new HashMap<String, ArrayList<Integer>>();
    errorCount = 0;
    errorList = new ArrayList<String>();
    symtab = new HashMap<String, Value>();
    relocationDictionary = new ArrayList<Integer>();
    publicList = new ArrayList<String>();
    externList = new ArrayList<String>();
    symtab.put("SP", new Value.Defined(0));
    sax = this;
  }

  private String label;   // the label at the beginning of a line, if any
  public int LC;          // the current location counter

  // evaluate an expression,
  //returning null if there's an error
  private Value eval() {
    try {
      Value v = new Eval(this).eval();
      return v;
    } catch (Exception e) {
      // we get here in case of an expression syntax error
      return null;
    }
  }

  // beginning of line
  FSMState sax_INIT = new FSMState() {
    public FSMState next() { 
      resetErrorList();
      label = null;
      Token tok = CUR();
      if (tok.val == Token.Val.EOF) {
        if(pass==2){
          lst.format("%4s:", "EOF");
          lst.format("%9d%n", LC);
        }
        return null; // end of this pass!
      }
      //pass two! start writing to listing file
      if(pass == 2){
        lst.format("%4d:", lio.getLineNumber());
        lst.format("%9d", LC);
      }

      if (tok.val == Token.Val.ID) {
        ADV();
        // this is in label position
        label = tok.str.toString();
        if(label != null)
          ll(tok.str.toString(), lio.getLineNumber());
        return sax_ID;
      }
      return sax_OP;
    }
  };

  FSMState sax_EAT = new FSMState() {
    public FSMState next() {
      err("... skipping ...");
      // avoid an extra FSM state -- just loop until
      // we see a NEWLINE
      while(true) {
        Token tok = CUR();
        if (tok.val == Token.Val.NEWLINE)
          return sax_NL;
        ADV();
      }
    }
  };

  FSMState sax_NL = new FSMState() {
    public FSMState next() {
      Token tok = CUR();
      if (tok.val == Token.Val.NEWLINE) {
        ADV();
        printErrorList();
        if(pass == 2 && !dwBool){
          try{
            //stupid number of booleans to format format listing file
            if(!operand && !op0){
              lst.format("%23s", " ");
            }else if(!operand){
              lst.format("%20s", " ");
            }else if(!op0){
              lst.format("%2s", " ");
            }else if(op0 && operand){
              lst.format("%3s", " ");
            }
            lst.format("%s", lio.getLine());
            
            ll(tok.str.toString(), lio.getLineNumber());
            
            //reset the bools to go again!
            op0 = false;
            operand = false;
          } catch(Exception e){
            System.err.println(e + ": cannot read line");
            System.exit(1);
          }
          //line break
          lst.format("%n");
        }
        dwBool =false;

        return sax_INIT; // get another line
      }
      err(tok + ": unexpected token");
      return sax_EAT;
    }
  };

  FSMState sax_ID = new FSMState() {
    public FSMState next() {
      Token tok = CUR();
      // label is guaranteed to be non-null here
      if (tok.val == Token.Val.EQU) {
        ADV();
        Value val = eval();
        if (val == null)
          return sax_EAT; // syntax error in expression    
        ll(tok.str.toString(), lio.getLineNumber());
        symtabPut(label, val);
        // System.err.println(label + " EQU " + val);
        return sax_NL;
      }
      if (tok.val == Token.Val.COLON) {
        ADV();
        //FIXME
        ll(tok.str.toString(), lio.getLineNumber());
        symtabPut(label, new Value.Defined(1,LC));
        //TODO fixed?????????????????????????
        return sax_OP;
      }
      return sax_EAT;
    }
  };

  FSMState sax_OP = new FSMState() {
    public FSMState next() {
      Token tok = CUR();
      if (tok.val == Token.Val.OP0) {
        //generate object module information on pass 2
        emitOp(tok.opcode);
        ADV();
        return sax_NL;
      }
      if (tok.val == Token.Val.OP1) {
        ADV();
        //generate object module information on pass 2
        emitOp(tok.opcode);
        // System.out.println("... evaluating the operand ...");
        Value val = eval(); // evaluate the operand
        // we should have seen one operand! So this must be an error.
        if (val == null)
          return sax_EAT; // syntax error
        //publish information to object module
        emitVal(val);
        return sax_NL;
      }
      if (tok.val == Token.Val.DW) {
        ADV();
        return sax_DWS; 
      }
      if (tok.val == Token.Val.DS) {
        ADV();
        Value val = eval();
        if (val == null)
          return sax_EAT;
        if (! val.isAbsolute())
          err(val + ": DS argument not absolute");
        else
          emitDS(val.getVal());
        return sax_NL;
      }
      if (tok.val == Token.Val.EXTERN) {
        ADV();
        return sax_EXTERNS;
      }
      if (tok.val == Token.Val.PUBLIC) {
        ADV();
        return sax_PUBLICS;
      }
      if (tok.val == Token.Val.ENTRY) {
        ADV();
        return sax_ENTRY;
      }
      return sax_NL;
    }
  };

  // handle one or more DW entries
  public FSMState sax_DWS = new FSMState() {
    public FSMState next() {
      Token tok = CUR();
      if (tok.val == Token.Val.STRING) {
        // System.err.println("... string=" + tok.str + " ...");
        ADV();
        //explode the string
        processString(tok.str.toString());
        // emit the individual characters in the string
      } else {
        Value val = eval();
        emitVal(val);
        if (val == null) // expression syntax error
          return sax_EAT;
        if(!dwBool){
          //emitVal(val);
          if(pass == 2)
            lst.format("%s", lio.getLine());
          dwBool = true;
        } else {
          if(pass == 2)
            lst.format("%5d", LC);
          //emitVal(val);
        }
      }


      // loop back for more DWs
      tok = CUR();
      if (tok.val == Token.Val.COMMA) {
        ADV();
        if(pass == 2)
          lst.format("%n%6s", " ");
        return this;
      }
      if(pass == 2)
        lst.format("%n");
      return sax_NL;
    }
  };

  // handle one or more EXTERN declarations
  public FSMState sax_EXTERNS = new FSMState() {
    public FSMState next() {
      Token tok = CUR();
      if (tok.val != Token.Val.ID) {
        err(tok + ": EXTERN label expected");
        return sax_EAT;
      }
      ADV();
      String id = tok.str.toString();
      if (pass == 1) {
        symtabPut(id, new Value.Extern(id)); /* FIXME */
        //TODO fixed???????
        externList.add(id);
      }
      // loop back for more EXTERNs
      tok = CUR();
      if (tok.val == Token.Val.COMMA) {
        ADV();
        return this;
      }
      return sax_NL;
    }
  };

  // handle one or more PUBLIC declarations
  public FSMState sax_PUBLICS = new FSMState() {
    public FSMState next() {
      Token tok = CUR();
      if (tok.val != Token.Val.ID) {
        err(tok + ": PUBLIC label expected");
        return sax_EAT;
      }
      ADV();
      String id = tok.str.toString();
      if (pass == 1)
        //place the LC to the relocation dictionary
        addToPublics(id);
      // loop back for more PUBLICSs
      tok = CUR();
      if (tok.val == Token.Val.COMMA) {
        ADV();
        return this;
      }
      return sax_NL;
    }
  };

  // handle an ENTRY declaration
  public FSMState sax_ENTRY = new FSMState() {
    public FSMState next() {
      Token tok = CUR();
      if (tok.val != Token.Val.ID) {
        err(tok + ": ENTRY label expected");
        return sax_EAT;
      }
      ADV();
      String id = tok.str.toString();
      /* FIXME:
         0. do not allow a local label in an ENTRY declaraction
         1. on pass 1, do not allow more than one ENTRY declaration
         2. on pass 2, consider it an error if the value
         associated with the ENTRY label is undefined
         or not relocatable
       */
      if(pass == 1){
        if(entryID != null){
          err(tok + " ENTRY already defined as: " + entryID);
          return sax_EAT;
        }
        entryID=id;
      }
      if(pass == 2){
        if(entryID == null || (symtabGet(entryID)).isAbsolute()){
          err(tok + " ENTRY must not be null, or absolute");
          return sax_EAT;
        }
        //TODO print to object module????????
      }
      return sax_NL;
    }
  };

  public Token CUR() {
    Token t = lztok.cur();
    // System.out.println("token: [" + t + "]");
    return t;
  }

  public void ADV() {
    lztok.adv();
  }

  // emit an opcode
  //used in object module formation
  public void emitOp(int op) {
    // use legacy opcodes (no CALLS instruction
    op0 =true;
    if (legacy) {
      if (op == 23)
        err(op + ": illegal legacy opcode");
      else if (op > 23)
        op--;
    }
    if (pass == 2) {
      System.out.println(op);
      lst.format(" %2d", op);
    }
    LC++;
  }

  // emit an operand value -- may be absolute, relative or extern
  public void emitVal(Value v) {
    if (pass == 2) {
      if (v == Value.UNDEF) {
        err(v + ": undefined operand value");
      } else {
        if (v.isRelative()){
          addReloc(LC); // add to relocation dictionary
        } else if (v.isExtern()){
          v.addFixup(LC);  // add LC to the EXTERN fixup list
        }
        if(op0 == false){
          lst.format("%3s", " ");
        }
        lst.format("%16d%s", v.getVal(), v.isRelative() ? "R" : " " );
        System.out.println(v.getVal());
      }	    

      operand=true;
    }
    LC++;
  }

  // emit a DS value (a colon followed by a positive integer value)
  public void emitDS(int ds) {
    /* FIXME */
    //TODO FIXED?????????????????
    if(pass == 2){
      System.out.println(":" + ds);
    }
    LC += ds;
  }

  // Note: All labels that appear in label position will be
  // in the symbol table at the end of pass 1.
  // In pass 2, all labels that appear in label position will
  // be re-evaluated (possibly still UNDEF).
  // This method does not handle local labels properly /* FIXME */
  public void symtabPut(String s, Value v) {
    Value vv;
    // System.err.println("... put("+s+","+v+") ...");
    if(pass ==1){
      //trim label length to 30 chars only
      if(s.length() > 30){
        s=s.substring(0,30);
      }
      //deal with local labels
      if(s.charAt(0) == '@'){
        if(MRNL == null){
          err(s + ": cannot set local label without a non local");
        }          
        s = MRNL + s;
      }else{
        //s is non-local
        vv = symtab.get(s);
        if (vv != null && pass == 1){
          err(s + ": multiply defined label");
        }
        MRNL = s;
      }
      vv = symtab.get(s);
      if (vv != null && pass == 1){
        err(s + ": multiply defined label");
      }
      symtab.put(s,v);
    }
  }

  public Value symtabGet(String s) {

    // check if greater than 30... b/c I'm nervous :\
    if(s.length() >30){
      s = s.substring(0,30);
    }

    if((s.toUpperCase()).equals("SP"))
      s = "SP";

    //check if the label is local, and if it is wrapped by a 
    //non-local label
    if(isLocal(s)){
      if(MRNL!= null){
        //append the label with its non-local counterpart
        s = MRNL + s;
      }else{
        //local label must have a non-local label before it
        err(s + ": no non-local label defined");
      }
    }
    Value v = symtab.get(s);
    if (v == null)
      v = Value.UNDEF;
    // System.err.println("... get("+s+") => "+v+" ...");
    return v;
  }

  public void processString(String s) {
    int len = s.length();

    for (int i=0 ; i<len ; i++) {
      //place the ascii value of the character in the module
      Value v = new Value.Defined((int)s.charAt(i));
      emitVal(v);
      if(!dwBool){
        lst.format("%s", lio.getLine());
        lst.format("%n%9s", " ");
        dwBool = true;
      }else if(i == len-1){
        lst.format("%5d", LC, " ");
      }else{
        lst.format("%5d", LC);
        lst.format("%n%9s", " ");
      }
    }
  }

  public void addReloc(int lc) {
    relocationDictionary.add(lc);
  }

  public void addToPublics(String s) {
    publicList.add(s);
  }

  public void msg(String s){
    System.out.println(s);
  }

  public String formatMsg(String s) {
    return "[" + lio.getLineNumber() + "]>>> " + s;
  }

  // add an error string to the error list and increment the error counter
  public void err(String s) {
    errorList.add(formatMsg(s));
    errorCount++;
  }

  public void resetErrorList() {
    errorList.clear();
  }


  public void printErrorList() {
    for (String s : errorList)
      System.err.println(s);
  }

  public boolean isLocal(String s) {
    return s.charAt(0) == '@';
  }

  public void rdrOpen() {
    try {
      rdr = new FileReader(prog);
    } catch (Exception e) {
      System.err.println(e + " -- assembly aborted");
      System.exit(1);
    }
    try {
      BR = new BufferedReader(new FileReader(prog));
    } catch (Exception e1) {
      System.err.println(e1 + " -- assembly aborted");
      System.exit(1);
    }
  }


  public void rdrClose() {
    try {
      rdr.close();
    } catch (Exception e) {
      System.err.println(e + " -- assembly aborted");
      System.exit(1);
    }
  }

  // if the '-o out' command line option is given,
  // redirect System.out to the corresponding PrintStream 
  public void run() {

    //redirect the object module to a file if the -o flag is set
    if (out != null) {
      try {
        System.setOut(new PrintStream(out));
      } catch (FileNotFoundException e) {
        System.err.println(out+": cannot create object file for writing");
        System.exit(1);
      }
    }

    //redirect listing file to a file, is the -l flag is set
    if(list != null){
      try {
        lst = new PrintStream(list);
      } catch (FileNotFoundException e){   
        System.err.println(lst+ ": cannot create listing file for writing");
        System.exit(1);
      }
    }else{
      //hackish way to deal with no -l param
      try{
        File f = new File("list");
        lst = new PrintStream(f){
          public PrintStream format(String format, Object ... args){
            return null; 
          }
        };
        f.delete();
      }catch(FileNotFoundException e){
        System.err.println(lst+ ": cannot create temp output file for listing");
        System.exit(1);
      }
    }

    entryID = null;
    // do two passes
    for (pass=1 ; pass <= 2 ; pass++) {
      errorCount = 0;
      if (pass == 2) {

        //begin printing the listing file (header only)
        printList();
        String xx = legacy ? "-" : "+";
        System.out.println("%SXX" + xx + "Object Module");
        System.out.println("# Object module for file " + prog);
        System.out.println(LC + " text length"); // LC from pass 1!
        System.out.println("% text");
      }
      LC = 0;
      lio = null;
      rdrOpen();
      lio = new LineIO(rdr);
      tokio = new TokenReader(lio);
      lztok = new Lazy<Token>(tokio);
      FSM.run(sax_INIT);
      rdrClose();
      if (errorCount > 0) {
        System.err.println("Pass "
            +pass
            +" errors encountered ... exiting");
        System.exit(2);
      }
    }
    System.out.println("% relocation dictionary");
    for (Integer r : relocationDictionary)
      System.out.println(r);
    System.out.println("% ENTRY, EXTERN, and PUBLIC references");
    /* FIXME */
    //Place EXTERN and PUBLIC references

    //Print ENTRY
    if(entryID != null)
      System.out.println("ENTRY " + entryID + " " + (symtab.get(entryID)).getVal());

    //Print EXTERNs
    for(String s : externList){ 
      if((((Value.Extern)symtab.get(s)).fixupList).isEmpty())
        continue;

      String out = "EXTERN " + s;
      for(Integer i : ((Value.Extern)symtab.get(s)).fixupList){
        out += " " + i;
      }
      System.out.println(out);
    } 

    //Print PUBLIC
    for(String s : publicList){
      String out = "PUBLIC " + s + " " + symtab.get(s).getVal();
      out += " " +(symtab.get(s)).getVal();
      System.out.println(out);  
    }


    System.out.println("% end of object module");

    //print header for cross reference list
    lst.format("%n%n%n%s%n","Cross Reference List");
    lst.format("%s%n","--------------------");
    lst.format("%-30s%7s   %12s%n","Symbol", "Value", "Line Numbers");
    lst.format("%-30s%7s   %12s%n","------", "-----", "------------");

    TreeSet<String> ordered = new TreeSet<String>(symtab.keySet());
    List<Integer> temp;
    //the meat of the cross refernce list
    for(String s : ordered){
      lst.format("%-30s", s);
      Value cur = symtab.get(s);
      if(cur.isRelative()){
        lst.format("%7sR|", cur.getVal());
      }else{
        lst.format("%7s |", cur.getVal());
      }
      temp = labelToLineNo.get(s);
      if(temp == null){
        lst.format("%n");
        continue;
      }

      Collections.sort(temp);

      for(Integer i : temp){
        lst.format(" %d ", i);
      }
      lst.format("%n");

    }

  }

  public void printList(){
    lst.format("%-12s%-2s  %-2s %15s  %-30s%n", "Line", "LC", "Op", "Operand", "Source Line");
    lst.format("%-12s%-2s  %-2s %15s  %-30s%n", "----", "--", "--", "-------", "-----------");

  }

  public void ll(String label, Integer lineNo){
    if(labelToLineNo.get(label) == null){
      ArrayList<Integer> temp = new ArrayList<Integer>();
      temp.add(lineNo);
      labelToLineNo.put(label, temp);
    } else {
      if(!labelToLineNo.get(label).contains(lineNo))
        labelToLineNo.get(label).add(lineNo);
    }
  }

  public static void main(String [] args) {
    Getopt g = new Getopt("Sax", args, "l:o:xm");
    String prog = null;
    String list = null;
    String out = null;
    boolean legacy = false;
    int c;
    String usage = "usage: sax [-x] [-l <list>] [-o <obj>] <prog>";

    while ((c = g.getopt()) != -1) {
      switch((char)c) {
        case 'l': list = g.getOptarg(); break;
        case 'o': out = g.getOptarg(); break;
        case 'x': legacy = true; break;
        default:
                  System.err.println(usage);
                  System.exit(1);
      }
    }
    int optind = g.getOptind();
    if (optind != args.length-1) {
      System.err.println(usage);
      System.exit(1);
    }
    prog = args[optind];
    // System.err.println("list=" + list + " out=" + out + " prog=" + prog);
    Sax sax = new Sax(list, out, prog, legacy);
    sax.run();
  }
}
