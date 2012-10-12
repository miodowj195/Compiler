import java.util.*;

public class Token {

  public enum Val {
    EOF,	// end-of-file
      OP0,	// all pcodes with zero operands
      OP1,	// all opcodes with one operand
      DS,	// define storage
      DW,	// define word(s)
      EQU,
      PUBLIC,
      EXTERN,
      ENTRY,
      PLUS,	// '+'
      MINUS,	// '-'
      TIMES,	// '*'
      DIVIDE,	// '/'
      MOD,	// '%'
      LPAREN,	// '('
      RPAREN,	// ')'
      COMMA,	// ','
      COLON,	// ':'
      STRING,	// "..."
      ID,	// identifier
      INT,	// octal, decimal, hex, or character ('.)
      DOLLAR,	// '$'
      NEWLINE,// '\n'
      ERROR	// anything else (including error strings, malformed ints, etc)
  }

  public Val val;          // the value of this token
  public StringBuffer str; // the String contents of this token
  public int lno;          // the line number where this token starts
  public int opcode;       // the opcode of an OP0 or OP1 token

  public Token() {
    val = Val.ERROR;     // assume the worst
    str = new StringBuffer();
    lno = 0;
    opcode = 0;
  }

  public Token (Val val, StringBuffer str, int lno, int opcode) {
    this.val = val;
    this.str = str;
    this.lno = lno;
    this.opcode = opcode;
  }

  public Token (Val val, String s, int lno, int opcode) {
    this.val = val;
    this.str = new StringBuffer(s);
    this.lno = lno;
    this.opcode = opcode;
  }

  public String toString() {
    return "val="+val+" str="+str+" lno="+lno+" opcode="+opcode;
  }
}
