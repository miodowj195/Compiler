import cis463.util.*;
import cis463.fsm.*;
import java.util.*;

/**
 * A scanner for SAX tokens
 */
public class TokenReader implements StreamReader<Token> {

  private final int MAXIDLEN = 30;     // IDs are limited to 30 characters
  private LineIO lio;                  // a StreamReader<Character>
  private Lazy<Character> lzyin;       // a Lazy<Character> based on lio
  private static final char NL = '\n'; // the newline character

  // this variable is referenced in each FSMState method
  // and is returned as the value of the read() method for this class
  private Token tok;

  // construct an instance of the TokenReader based on a LineIO object
  public TokenReader(LineIO lio) {
    this.lio = lio;
    lzyin = new Lazy<Character>(lio);
  }

  // the FSM states that drive the finite state machine token scanner

  // the initial state -- all invocations of the scanner FSM start here
  private FSMState tok_INIT = new FSMState() {
    public FSMState next() {
      Character ch = CUR();
      tok.lno = lio.getLineNumber(); // be optimistic
      if (ch == null) {
        // end of file
        tok.str.append("*EOF*");
        VAL(Token.Val.EOF);
        return null;
      }
      if (ch == NL) {
        AAV(ch, Token.Val.NEWLINE);
        tok.str = new StringBuffer("NL"); // make it printable
        return null;
      }
      if (Character.isWhitespace(ch)) {
        ADV();
        // skip whitespace, but not a newline
        // must do this AFTER checking for NL, since
        // NL is considered whitespace
        return this;
      }
      if (Character.isLetter(ch) || ch == '@') {
        AA(ch);
        return tok_ID_OR_RESWORD;
      }
      if (ch == '0') {
        AA(ch);
        return tok_INT0;
      }
      if (Character.isDigit(ch)) {
        AA(ch);
        return tok_INT;
      }
      switch (ch) {
        case '+' : AAV(ch, Token.Val.PLUS); return null;
        case '-' : AAV(ch, Token.Val.MINUS); return null;
        case '*' : AAV(ch, Token.Val.TIMES); return null;
        case '/' : AAV(ch, Token.Val.DIVIDE); return null;
        case '%' : AAV(ch, Token.Val.MOD); return null;
        case ',' : AAV(ch, Token.Val.COMMA); return null;
        case ':' : AAV(ch, Token.Val.COLON); return null;
        case '(' : AAV(ch, Token.Val.LPAREN); return null;
        case ')' : AAV(ch, Token.Val.RPAREN); return null;
        case '$' : AAV(ch, Token.Val.DOLLAR); return null;
        case ';'  : ADV(); return tok_COMMENT;
        case '\'' : AA(ch); return tok_CHAR;
        case '\"' : ADV(); return tok_STRING;
        default   : AAV(ch, Token.Val.ERROR); return null;
      }
    }
  };

  // the first digit is a zero -- it's either octal or hex
  private FSMState tok_INT0 = new FSMState() {
    public FSMState next() {
      Character ch = CUR();
      if (ch == 'x' || ch == 'X') {
        AA(ch);
        return tok_HEX;
      }
      return tok_OCT_DIGITS;
    }
  };

  // it's an octal number -- look for more digits
  private FSMState tok_OCT_DIGITS = new FSMState() {
    public FSMState next() {
      Character ch = CUR();
      if (isOctDigit(ch)) {
        AA(ch);
        return tok_OCT_DIGITS;
      }
      VAL(Token.Val.INT);
      return null;
    }
  };

  // it's a hexadecimal number -- there must be at least one hex digit
  private FSMState tok_HEX = new FSMState() {
    public FSMState next() {
      Character ch = CUR();
      if (isHexDigit(ch)) {
        AA(ch);
        return tok_HEX_DIGITS;
      }
      // no digits ;-(
      VAL(Token.Val.ERROR);
      return null;
    }
  };

  // found at least one hex digit -- get more
  private FSMState tok_HEX_DIGITS = new FSMState() {
    public FSMState next() {
      Character ch = CUR();
      /* FIXME */
      //FIXED??????
      if(isHexDigit(ch)){
        AA(ch);
        return tok_HEX_DIGITS;
      }
      VAL(Token.Val.INT);
      return null;
    }
  };

  // it's a nonzero decimal integer -- get more decimal digits
  private FSMState tok_INT = new FSMState() {
    public FSMState next() {
      Character ch = CUR();
      /* FIXME */
      //FIXED?????
      if(ch>='0' && ch<='9'){
        AA(ch);
        return tok_INT;
      }
      VAL(Token.Val.INT);
      return null;
    }
  };

  // The previous character was a single quote -- find the next one.
  // After leaving this state, the token StringBuffer should contain
  // exactly the following:
  //        '.
  // where . is replaced by the current printable character.
  // If the current character is not printable, return the ERROR token.
  private FSMState tok_CHAR = new FSMState() {
    public FSMState next() {
      /* FIXME */
      // FIXED??????
      Character ch = CUR();
      if(isPrint(ch)){
        AA(ch);	
        VAL(Token.Val.INT);
        return null;
      }
      VAL(Token.Val.ERROR);
      return null;
    }
  };

  private FSMState tok_ID_OR_RESWORD = new FSMState() {
    public FSMState next() {
      Character ch = CUR();
      if (isIDChar(ch)) {
        AA(ch);
        return this; // loop
      }
      // truncate long identifiers
      String label = tok.str.toString();
      // An ID is truncated to MAXIDLEN
      if (label.length() > MAXIDLEN) {
        tok.val = Token.Val.ID;
        tok.str = new StringBuffer(label.substring(0,MAXIDLEN));
      } else {
        // Check to see if it's a (pseudo)op or an ID.
        Optab.check(tok);
      }
      return null;
    }
  };

  private FSMState tok_STRING = new FSMState() {
    public FSMState next() {
      Character ch = CUR();
      if (ch == '"') {
        ADV();
        // end of the string
        VAL(Token.Val.STRING);
        return null;
      }
      if (isPrint(ch)) {
        if (ch == '\\') {
          // escape char
          ADV();
          return tok_ESC;
        }
        AA(ch);
        return this;
      }
      throw
        new RuntimeException("non-terminated or malformed string");
      // VAL(Token.Val.ERROR);
      // return null;
    }
  };

  private FSMState tok_ESC = new FSMState() {
    public FSMState next() {
      Character ch = CUR();
      /* FIXME */
      // FIXED?
      if(isPrint(ch)){
        if(ch == 'b'){
          AA('\b');
          return tok_STRING;
        }
        if(ch == 't'){
          AA('\t');
          return tok_STRING;
        }
        if(ch == 'n'){
          AA('\n');
          return tok_STRING;
        }
        if(ch == 'f'){
          AA('\f');
          return tok_STRING;
        }
        if(ch == 'r'){
          AA('\r');
          return tok_STRING;
        }
        if(ch == '\''){
          AA('\'');
          return tok_STRING;
        }
        if(ch == '"'){
          AA('"');
          return tok_STRING;
        }
        if(ch == '\\'){
          AA('\\');
          return tok_STRING;
        }
      }
      APPEND('\\');
      APPEND(ch);
      VAL(Token.Val.ERROR);
      return null;
    }
  };

  private FSMState tok_COMMENT = new FSMState() {
    public FSMState next() {
      Character ch = CUR();
      /* FIXME */
      // FIXED???????
      if(ch != '\n'){
        ADV();
        return tok_COMMENT;	
      }		
      return tok_INIT; // a comment isn't a token!
    }
  };

  // gets the current token
  private Character CUR() {
    return lzyin.cur();
  }

  // ADVances the lazy input to the next item
  private void ADV() {
    lzyin.adv();
  }

  // Appends the character ch to the current token buffer
  // and advances to the next character
  private void AA(Character ch) {
    tok.str.append(ch);
    lzyin.adv();
  }

  // Advance, Append, and set Value
  private void AAV(Character ch, Token.Val val) {
    lzyin.adv();
    tok.str.append(ch);
    tok.val = val;
  }

  // APPENDs the character ch to the current token buffer
  private void APPEND(Character ch) {
    tok.str.append(ch);
  }

  // sets the VALue of this token to v
  private void VAL(Token.Val v) {
    tok.val = v;
  }

  // returns true if ch represents an ASCII printable character
  // with values in the range 32 to 126.
  private static boolean isPrint(Character ch) {
    if (ch == null)
      return false;
    /* FIXME */
    //FIXED?????????
    if(ch>31 && ch<127)
      return true;

    return false;
  }

  private static boolean isDigit(Character ch) {
    return Character.isDigit(ch);
  }

  private static boolean isHexDigit(Character ch) {
    return Character.digit(ch, 16) >= 0;
  }

  private static boolean isOctDigit(Character ch) {
    return Character.digit(ch, 8) >= 0;
  }

  // returns true if ch is a letter, digit, or underscore
  private static boolean isIDChar(Character ch) {
    if (ch == null)
      return false;
    return Character.isLetterOrDigit(ch) || ch == '_';
  }

  // run the FSM on the current input, starting with state tok_INIT
  public Token read() {
    tok = new Token();
    FSM.run(tok_INIT); // initialize and run the FSM
    return tok;
  }
}
