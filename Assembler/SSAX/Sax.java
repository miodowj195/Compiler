import cis463.util.*;
import cis463.fsm.*;
import java.util.*;
import java.io.*;
import gnu.getopt.Getopt;

public class Sax {


    public LineIO lio;
    public FileReader rdr;
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

    public Sax (String list,
		String out,
		String prog,
		boolean legacy) {
	this.list = list;
	this.out = out;
	this.prog = prog;
	this.legacy = legacy;
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
		    return null; // end of this pass!
		}
		if (tok.val == Token.Val.ID) {
		    ADV();
		    // this is in label position
		    label = tok.str.toString();
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
		    symtabPut(label, val);
		    // System.err.println(label + " EQU " + val);
		    return sax_NL;
		}
		if (tok.val == Token.Val.COLON) {
		    ADV();
		    // symtabPut(label, ???); /* FIXME */
		    return sax_OP;
		}
		return sax_EAT;
	    }
	};

    FSMState sax_OP = new FSMState() {
	    public FSMState next() {
		Token tok = CUR();
		if (tok.val == Token.Val.OP0) {
		    emitOp(tok.opcode);
		    ADV();
		    return sax_NL;
		}
		if (tok.val == Token.Val.OP1) {
		    ADV();
		    emitOp(tok.opcode);
		    // System.out.println("... evaluating the operand ...");
		    Value val = eval(); // evaluate the operand
		    if (val == null)
			return sax_EAT; // syntax error
		    emitVal(val);
		    return sax_NL;
		}
		if (tok.val == Token.Val.DW) {
		    ADV();
		    return sax_DWS; // this handles multiple DW operands
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
		    processString(tok.str.toString());
		    // emit the individual characters in the string
		} else {
		    Value val = eval();
		    if (val == null) // expression syntax error
			return sax_EAT;
		    emitVal(val);
		}
		// loop back for more DWs
		tok = CUR();
		if (tok.val == Token.Val.COMMA) {
		    ADV();
		    return this;
		}
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
		    // symtabPut(id, ???); /* FIXME */
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
    public void emitOp(int op) {
	// use legacy opcodes (no CALLS instruction)
	if (legacy) {
	    if (op == 23)
		err(op + ": illegal legacy opcode");
	    else if (op > 23)
		op--;
	}
	if (pass == 2) {
	    System.out.println(op);
	}
	LC++;
    }
    
    // emit an operand value -- may be absolute, relative or extern
    public void emitVal(Value v) {
	if (pass == 2) {
	    if (v == Value.UNDEF) {
		err(v + ": undefined operand value");
	    } else {
		if (v.isRelative())
		    addReloc(LC); // add to relocation dictionary
		else if (v.isExtern())
		    v.addFixup(LC);  // add LC to the EXTERN fixup list
		System.out.println(v.getVal());
	    }	    
	}
	LC++;
    }

    // emit a DS value (a colon followed by a positive integer value)
    public void emitDS(int ds) {
	/* FIXME */
	LC += ds;
    }

    // Note: All labels that appear in label position will be
    // in the symbol table at the end of pass 1.
    // In pass 2, all labels that appear in label position will
    // be re-evaluated (possibly still UNDEF).
    // This method does not handle local labels properly /* FIXME */
    public void symtabPut(String s, Value v) {
	// System.err.println("... put("+s+","+v+") ...");
	Value vv = symtab.get(s);
	if (vv != null && pass == 1)
	    err(s + ": multiply defined label");
	else
	    symtab.put(s,v);
    }

    // This method does not handle local labels properly /* FIXME */
    public Value symtabGet(String s) {
	Value v = symtab.get(s);
	if (v == null)
	    v = Value.UNDEF;
	// System.err.println("... get("+s+") => "+v+" ...");
	return v;
    }

    public void processString(String s) {
	int len = s.length();
	for (int i=0 ; i<len ; i++) {
	    // Value v = ???; /* FIXME */
	    emitVal(v);
	}
    }

    public void addReloc(int lc) {
	relocationDictionary.add(lc);
    }

    public void addToPublics(String s) {
	publicList.add(s);
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
	if (out != null) {
	    try {
		System.setOut(new PrintStream(out));
	    } catch (FileNotFoundException e) {
		System.err.println(out+": cannot create object file for writing");
		System.exit(1);
	    }
	}
	lst = System.err; /* FIXME */
	entryID = null;
	// do two passes
	for (pass=1 ; pass <= 2 ; pass++) {
	    errorCount = 0;
	    if (pass == 2) {
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
	System.out.println("% end of object module");
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
