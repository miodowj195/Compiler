import cis463.util.*;
import cis463.fsm.*;
import java.util.*;

/**
 * a TokenReader for Pascal tokens -- this tokenizes all reserved
 * words found in the Token.java class, binary and unary operands
 * as well as identifiers in compliance with the pascal specification
 * given in class. All other (non-whitespace) strings are considered
 * error tokens.
 *
 *@author Jonathan Midoownik
 *@see Token.java
 */
public class TokenReader implements StreamReader<Token> {

  private LineIO lio;              // a StreamReader<Character>
  private Lazy<Character> lzyin;   // a Lazy<Character> based on lio
  private static final char NL = '\n';    // the newline character
  // the map to contain reserved words, used as a lookup table
  private Map<String, Token.Val> reservedWord;

  public TokenReader(LineIO lio) {
    this.lio = lio;
    lzyin = new Lazy<Character>(lio);
   
    //Create a HashMap and populate it with reserced words
    reservedWord = new HashMap<String, Token.Val>();
    
    for(Token.Val v : EnumSet.range(Token.Val.AND, Token.Val.WITH))
      reservedWord.put(v.toString(), v);
  }


  // this variable is referenced in each FSMState method
  private Token tok;

  // the FSM states
  private FSMState t_INIT = new FSMState() {
    public FSMState next() {
      Character ch = lzyin.cur();
      if (ch == null) {
        // end of file
        tok.lno = lio.getLineNumber();
        tok.str.append("*EOF*");
        tok.val = Token.Val.EOF;
        return null;
      }

      /*
       * check to see if ch is a comment or error comment
       */
      if(ch == '{'){
        tok.lno = lio.getLineNumber();
        lzyin.adv();
        return t_COMMENT_OR_ERROR_COMMENT;
      }

      /**
       * check to see if this is a comment error comment
       * or a regular token
       */

      if(ch == '('){
        tok.lno = lio.getLineNumber();
        lzyin.adv();
        return t_POSSIBLE_PAREN_COMMENT;
      }
      /**
       * check to see if ch is a integer or real number
       */
      if(Character.isDigit(ch)){
        tok.lno = lio.getLineNumber();
        tok.str.append(ch);
        lzyin.adv();
        return t_POSSIBLE_INT_OR_REAL;
      }

      /*
       * check to see if ch is a comment
       */
      if(ch == '\''){
        tok.lno = lio.getLineNumber();
        lzyin.adv();
        return t_STRING_OR_ERROR_STRING;
      }

       /*
        * Test to see if ch is the begining of a identifier
        * or a reserved word
        */
       if (Character.isLetter(ch)) {
          tok.lno = lio.getLineNumber();
          tok.str.append(ch);
          lzyin.adv();
          return t_POSSIBLE_ID;
       }

       /*
        * Test to see if ch is whitespace
        */
       if (Character.isWhitespace(ch)){
          lzyin.adv();
          return t_INIT;
       }
      
      
      /*
       * Paramater: ch - The Character containing the token we are 
       *                 checking for.
       * Purpose: If here, no other conditions have been met, and
       *          ch MUST either be a single character token (eg +)
       *          or a two character token (eg >=)
       */
      switch(ch){
        case '+':
          tok.lno = lio.getLineNumber();
          tok.str.append("+");
          tok.val = Token.Val.PLUS;
          lzyin.adv();
          return null; 
        case '-':
          tok.lno = lio.getLineNumber();
          tok.str.append("-");
          tok.val = Token.Val.MINUS;
          lzyin.adv();
          return null; 
        case '*':
          tok.lno = lio.getLineNumber();
          tok.str.append("*");
          tok.val = Token.Val.TIMES;
          lzyin.adv();
          return null; 
        case '/':
          tok.lno = lio.getLineNumber();
          tok.str.append("/");
          tok.val = Token.Val.DIVIDE;
          lzyin.adv();
          return null; 
          //TODO possibly remove this case
       /* case '(':
          tok.lno = lio.getLineNumber();
          tok.str.append("(");
          tok.val = Token.Val.LPAREN;
          lzyin.adv();
          return null; */
        case ')':
          tok.lno = lio.getLineNumber();
          tok.str.append(")");
          tok.val = Token.Val.RPAREN;
          lzyin.adv();
          return null; 
        case '[':
          tok.lno = lio.getLineNumber();
          tok.str.append("[");
          tok.val = Token.Val.LBRACK;
          lzyin.adv();
          return null; 
        case ']':
          tok.lno = lio.getLineNumber();
          tok.str.append("]");
          tok.val = Token.Val.RBRACK;
          lzyin.adv();
          return null; 
        case '>':
          tok.lno = lio.getLineNumber();
          tok.str.append(">");
          tok.val = Token.Val.GT;
          lzyin.adv();
          return t_POSSIBLE_GE;
        case '<':
          tok.lno = lio.getLineNumber();
          tok.str.append("<");
          tok.val = Token.Val.LT;
          lzyin.adv();
          return t_POSSIBLE_NE_LE;
        case ':':
          tok.lno = lio.getLineNumber();
          tok.str.append(":");
          tok.val = Token.Val.COLON;
          lzyin.adv();
          return t_POSSIBLE_ASSIGN;
        case '=':
          tok.lno = lio.getLineNumber();
          tok.str.append("=");
          tok.val = Token.Val.EQU;
          lzyin.adv();
          return null;
        case '.':
          tok.lno = lio.getLineNumber();
          tok.str.append(".");
          tok.val = Token.Val.DOT;
          lzyin.adv();
          return t_POSSIBLE_DOTDOT;
        case ',':
          tok.lno = lio.getLineNumber();
          tok.str.append(",");
          tok.val = Token.Val.COMMA;
          lzyin.adv();
          return null;
        case ';':
          tok.lno = lio.getLineNumber();
          tok.str.append(";");
          tok.val = Token.Val.SEMI;
          lzyin.adv();
          return null;
        case '^':
          tok.lno = lio.getLineNumber();
          tok.str.append("^");
          tok.val = Token.Val.UPARROW;
          lzyin.adv();
          return null;

      }

      //anything else MUST be an error token
      tok.lno = lio.getLineNumber();
      tok.str.append(ch);
      tok.val = Token.Val.ERROR;
      lzyin.adv();
      return null;

      // ignore anything else
      //return t_INIT; // ... could loop instead ...
    }
  };

  private FSMState t_POSSIBLE_PAREN_COMMENT = new FSMState(){
    public FSMState next(){
      Character ch = lzyin.cur();
      if(ch == '*'){
        lzyin.adv();
        return t_PAREN_COMMENT_OR_ECOMMENT;
      }
      //lzyin.put(ch);
      tok.val = Token.Val.LPAREN;
      tok.str.append("(");
      return null;
    }
  };

  private FSMState t_PAREN_COMMENT_OR_ECOMMENT = new FSMState(){
    public FSMState next(){
      Character ch = lzyin.cur();
      if(ch == null){
        tok.val = Token.Val.ECOMMENT;
        return null;
      }

      if(ch == '\n'){
        lzyin.adv();
        return t_PAREN_MULTILINE_COMMENT_OR_ECOMMENT;
      }

      if(ch == '*'){
        lzyin.adv();
        return t_PAREN_POSSIBLE_END_OF_COMMENT;
      }
      
      tok.str.append(ch);
      lzyin.adv();
      return t_PAREN_COMMENT_OR_ECOMMENT;
    }
  };

  private FSMState t_PAREN_POSSIBLE_END_OF_COMMENT = new FSMState(){
    public FSMState next(){
      Character ch = lzyin.cur();

      if(ch == ')'){
        tok.val = Token.Val.COMMENT;
        lzyin.adv();
        return null;
      }

      if(ch == null){
        tok.val = Token.Val.ECOMMENT;
      }
      
      tok.str.append("*");
      return t_PAREN_COMMENT_OR_ECOMMENT;
    }
  };


  private FSMState t_PAREN_MULTILINE_COMMENT_OR_ECOMMENT = new FSMState(){
    public FSMState next(){
      Character ch = lzyin.cur();
      if(ch == null){
        tok.val = Token.Val.ECOMMENT;
        return null;
      }
      if(ch == '*'){
        lzyin.adv();
        return t_MULTI_PAREN_POSSIBLE_END_OF_COMMENT;
      }

      lzyin.adv();
      return t_PAREN_MULTILINE_COMMENT_OR_ECOMMENT;
    }
  };

  private FSMState t_MULTI_PAREN_POSSIBLE_END_OF_COMMENT = new FSMState(){
    public FSMState next(){
      Character ch = lzyin.cur();
      if(ch == null){
        tok.val = Token.Val.ECOMMENT;
        return null;
      }

      if(ch == ')'){
        tok.val = Token.Val.COMMENT;
        lzyin.adv();
        return null;
      }

      return t_PAREN_MULTILINE_COMMENT_OR_ECOMMENT;

    }
  };

  private FSMState t_MULTI_POSSIBLE_END_OF_COMMENT= new FSMState(){
    public FSMState next(){
      Character ch = lzyin.cur();
      if(ch == '}'){
        tok.val = Token.Val.COMMENT;
        lzyin.adv();
        return null;
      }
      if(ch == null){
        tok.val = Token.Val.ECOMMENT;
        return null;
      }

      return t_PAREN_MULTILINE_COMMENT_OR_ECOMMENT;
    }
  };

  private FSMState t_COMMENT_OR_ERROR_COMMENT = new FSMState(){
    public FSMState next(){
      Character ch = lzyin.cur();
      if(ch == null){
        tok.val = Token.Val.ECOMMENT;
        return null;
      }
      if(ch == '\n'){
        lzyin.adv();
        return t_MULTILINE_COMMENT_OR_ERROR_COMMENT;
      }
      if(ch == '}'){
        lzyin.adv();
        tok.val = Token.Val.COMMENT;
        return null;
      }
      tok.str.append(ch);
      lzyin.adv();
      return t_COMMENT_OR_ERROR_COMMENT;
    }
  };

  private FSMState t_MULTILINE_COMMENT_OR_ERROR_COMMENT = new FSMState(){
    public FSMState next(){
      Character ch = lzyin.cur();
      if(ch == null){
        tok.val = Token.Val.ECOMMENT;
        return null;
      }
  
      if(ch == '\n'){
        lzyin.adv();
        return t_MULTILINE_COMMENT_OR_ERROR_COMMENT;
      }


      if(ch == '}'){
        tok.val = Token.Val.COMMENT;
        lzyin.adv();
        return null;
      }
      lzyin.adv();
      return t_MULTILINE_COMMENT_OR_ERROR_COMMENT;
    }
  };
  
  private FSMState t_STRING_OR_ERROR_STRING = new FSMState(){
    public FSMState next(){
      Character ch = lzyin.cur();
      
      if(ch == null){
        tok.val = Token.Val.ESTRING;
        return null;
      }

      if(ch == '\''){
        lzyin.adv();
        ch = lzyin.cur();
        if(ch == '\''){
          tok.str.append('\'');
          lzyin.adv();
          return t_STRING_OR_ERROR_STRING;
        }
        tok.val = Token.Val.STRING;
        return null;
      }
      char c = (char)ch;
      if(c > 31 && c < 128){
         tok.str.append(ch);
         lzyin.adv();
         return t_STRING_OR_ERROR_STRING;
         }
      
      tok.val = Token.Val.ESTRING;
      return null;
    }
  };

  private FSMState t_POSSIBLE_INT_OR_REAL = new FSMState() {
    public FSMState next(){
      Character ch = lzyin.cur();
      if(ch == '.'){
        lzyin.adv();
        return t_POSSIBLE_REAL_OR_SCI;
      }

      if(Character.isDigit(ch)){
        tok.str.append(ch);
        lzyin.adv();
        return t_POSSIBLE_INT_OR_REAL;
      }
      tok.val = Token.Val.INT;
      return null;
    }
  };
  
  private FSMState t_POSSIBLE_REAL_OR_SCI = new FSMState(){
    public FSMState next(){
        Character ch = lzyin.cur();
      
        if(Character.isDigit(ch) && tok.str.length() == 1){
          tok.str.append(".");
          tok.str.append(ch);
          lzyin.adv();
          tok.val = Token.Val.REAL;
          return t_POSSIBLE_SCI;
        }

        if(Character.isDigit(ch)){
          tok.str.append(".");
          tok.str.append(ch);
          tok.val = Token.Val.REAL;
          lzyin.adv();
          return t_REAL;
        }

        //token is NOT a real
        tok.val = Token.Val.INT;
        lzyin.put('.');
        return null;
    }
  };

  private FSMState t_POSSIBLE_SCI = new FSMState(){
    public FSMState next(){
      Character ch = lzyin.cur();
     
      if(Character.isDigit(ch)){
        tok.str.append(ch);
        lzyin.adv();
        return t_POSSIBLE_SCI;
      }

      if(ch == 'e'){
        tok.str.append(ch);
        lzyin.adv();
        return t_POSSIBLE_SCI_W_E;
      }
      
      
      return null;
    }
  };

  private FSMState t_POSSIBLE_SCI_W_E = new FSMState(){
    public FSMState next(){
      Character ch = lzyin.cur();
      if(ch == '-'){
        tok.str.append(ch);
        lzyin.adv();
        return t_POSSIBLE_SIGNED_SCI_W_E;
      }
      if(Character.isDigit(ch)){
        tok.str.append(ch);
        lzyin.adv();
        return t_REAL;
      }
      
      tok.str.setLength(tok.str.length()-1);
      lzyin.put('e');
      return null;
    }
  };

  private FSMState t_POSSIBLE_SIGNED_SCI_W_E = new FSMState(){
    public FSMState next(){
      Character ch = lzyin.cur();
      if(Character.isDigit(ch)){
        tok.str.append(ch);
        lzyin.adv();
        return t_REAL;
      }
      tok.str.setLength(tok.str.length()-2);
      lzyin.put('-');
      lzyin.put('e');
      return null;
    }
  };

  private FSMState t_REAL = new FSMState(){
    public FSMState next(){
      Character ch = lzyin.cur();
      if(Character.isDigit(ch)){
        tok.str.append(ch);
        lzyin.adv();
        return t_REAL;
      }
      return null;
    }
  };

  private FSMState t_POSSIBLE_ID = new FSMState() {
    public FSMState next(){
      Character ch = lzyin.cur();
      if(Character.isLetter(ch) | Character.isDigit(ch) | ch == '_'){
        tok.str.append(ch);
        lzyin.adv();
        return t_POSSIBLE_ID;
      }
      
      if(tok.str.length()>9){
        tok.val = Token.Val.ID;
        return null;
      }

      Token.Val lookUp = reservedWord.get(tok.str.toString().toUpperCase());
      
      if(lookUp == null)
        tok.val = Token.Val.ID;
      else{
        tok.str.replace(0, tok.str.length(), 
            tok.str.toString().toUpperCase());
        tok.val = lookUp;
      }
      return null;
    }
  };

  private FSMState t_POSSIBLE_DOTDOT = new FSMState() {
    public FSMState next() {
      Character ch = lzyin.cur();
      if(ch == '.'){
        tok.str.append(".");
        tok.val = Token.Val.DOTDOT;
        lzyin.adv();
      }
      return null;
    }
  };

  private FSMState t_POSSIBLE_ASSIGN = new FSMState() {
    public FSMState next() {
      Character ch = lzyin.cur();
      if(ch == '='){
        tok.str.append("=");
        tok.val = Token.Val.ASSIGN;
        lzyin.adv(); 
      }
      return null;
    }
  };
  private FSMState t_POSSIBLE_GE = new FSMState() {
    public FSMState next() {
      Character ch = lzyin.cur();
      if(ch == '='){
        tok.str.append("=");
        tok.val = Token.Val.GE;
        lzyin.adv(); 
      }
      return null;
    }
  };

  private FSMState t_POSSIBLE_NE_LE = new FSMState() {
    public FSMState next() {
      Character ch = lzyin.cur();
      if(ch== '>'){
        tok.str.append(">");
        tok.val = Token.Val.NE;
        lzyin.adv();
      }
      else if(ch == '='){
        tok.str.append("=");
        tok.val = Token.Val.LE;
        lzyin.adv();
      }
      return null;

    }
  };


  public Token read() {
    tok = new Token();
    FSM.run(t_INIT); // initialize and run the FSM
    return tok;
  }
}