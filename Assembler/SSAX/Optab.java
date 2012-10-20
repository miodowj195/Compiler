import java.util.*;

public class Optab {

  private static class OptabEntry {
    public Token.Val val;
    public int opcode;

    public OptabEntry(Token.Val val, int opcode) {
      this.val = val;
      this.opcode = opcode;
    }
  }

  private static Map<String, OptabEntry> optab;

  static {
    optab = new HashMap<String,OptabEntry>();
    // create the entries here
    optab.put("DW", new OptabEntry(Token.Val.DW, 0));
    optab.put("DS", new OptabEntry(Token.Val.DS, 0));
    optab.put("EQU", new OptabEntry(Token.Val.EQU, 0));
    optab.put("PUBLIC", new OptabEntry(Token.Val.PUBLIC, 0));
    optab.put("EXTERN", new OptabEntry(Token.Val.EXTERN, 0));
    optab.put("ENTRY", new OptabEntry(Token.Val.ENTRY, 0));
    optab.put("BKPT", new OptabEntry(Token.Val.OP0, 0));
    optab.put("PUSH", new OptabEntry(Token.Val.OP1, 1));
    optab.put("PUSHV", new OptabEntry(Token.Val.OP1, 2));
    optab.put("PUSHS", new OptabEntry(Token.Val.OP0, 3));
    optab.put("INDIR", new OptabEntry(Token.Val.OP0, 3));
    optab.put("PUSHX", new OptabEntry(Token.Val.OP1, 4));
    optab.put("POP", new OptabEntry(Token.Val.OP1, 5));
    optab.put("POPS", new OptabEntry(Token.Val.OP0, 6));
    optab.put("POPX", new OptabEntry(Token.Val.OP1, 7));
    optab.put("DUPL", new OptabEntry(Token.Val.OP0, 8));
    optab.put("SWAP", new OptabEntry(Token.Val.OP0, 9));
    optab.put("OVER", new OptabEntry(Token.Val.OP0, 10));
    optab.put("DROP", new OptabEntry(Token.Val.OP0, 11));
    optab.put("ROT", new OptabEntry(Token.Val.OP0, 12));
    optab.put("TSTLT", new OptabEntry(Token.Val.OP0, 13));
    optab.put("TSTLE", new OptabEntry(Token.Val.OP0, 14));
    optab.put("TSTGT", new OptabEntry(Token.Val.OP0, 15));
    optab.put("TSTGE", new OptabEntry(Token.Val.OP0, 16));
    optab.put("TSTEQ", new OptabEntry(Token.Val.OP0, 17));
    optab.put("TSTNE", new OptabEntry(Token.Val.OP0, 18));
    optab.put("BNE", new OptabEntry(Token.Val.OP1, 19));
    optab.put("BT", new OptabEntry(Token.Val.OP1, 19));
    optab.put("BEQ", new OptabEntry(Token.Val.OP1, 20));
    optab.put("BF", new OptabEntry(Token.Val.OP1, 20));
    optab.put("BR", new OptabEntry(Token.Val.OP1, 21));
    optab.put("CALL", new OptabEntry(Token.Val.OP1, 22));
    optab.put("CALLS", new OptabEntry(Token.Val.OP0, 23));
    optab.put("RETURN", new OptabEntry(Token.Val.OP0, 24));
    optab.put("POPPC", new OptabEntry(Token.Val.OP0, 24));
    optab.put("RETN", new OptabEntry(Token.Val.OP1, 25));
    optab.put("HALT", new OptabEntry(Token.Val.OP0, 26));
    optab.put("ADD", new OptabEntry(Token.Val.OP0, 27));
    optab.put("SUB", new OptabEntry(Token.Val.OP0, 28));
    optab.put("MUL", new OptabEntry(Token.Val.OP0, 29));
    optab.put("DIV", new OptabEntry(Token.Val.OP0, 30));
    optab.put("MOD", new OptabEntry(Token.Val.OP0, 31));
    optab.put("OR", new OptabEntry(Token.Val.OP0, 32));
    optab.put("AND", new OptabEntry(Token.Val.OP0, 33));
    optab.put("XOR", new OptabEntry(Token.Val.OP0, 34));
    optab.put("NOT", new OptabEntry(Token.Val.OP0, 35));
    optab.put("NEG", new OptabEntry(Token.Val.OP0, 36));
    optab.put("ADDX", new OptabEntry(Token.Val.OP1, 37));
    optab.put("ADDSP", new OptabEntry(Token.Val.OP1, 38));
    optab.put("READ", new OptabEntry(Token.Val.OP0, 39));
    optab.put("PRINT", new OptabEntry(Token.Val.OP0, 40));
    optab.put("READC", new OptabEntry(Token.Val.OP0, 41));
    optab.put("PRINTC", new OptabEntry(Token.Val.OP0, 42));
    optab.put("TRON", new OptabEntry(Token.Val.OP0, 43));
    optab.put("TROFF", new OptabEntry(Token.Val.OP0, 44));
    optab.put("DUMP", new OptabEntry(Token.Val.OP0, 45));
  }

  public static final int MAXOPLEN = 6;

  // check the token t to see if it's an ID or (pseudo)op
  public static void check(Token t) {
    String s = t.str.toString().toUpperCase();
    if (s.length() > MAXOPLEN) {
      t.val = Token.Val.ID;
      return;
    }
    if (s.equals("SP")) {
      // it's an ID
      t.str = new StringBuffer("SP");
      t.val = Token.Val.ID;
      return;
    }
    // look it up in the op table
    OptabEntry op = optab.get(s);
    if (op == null) {
      // must be an ID
      t.val = Token.Val.ID;
      return;
    }
    t.val = op.val;
    t.opcode = op.opcode;
  }
}
