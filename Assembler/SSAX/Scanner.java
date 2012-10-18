import cis463.util.*;
import cis463.util.*;

// test the TokenReader class
public class Scanner {

  public static void main(String [] args) {
    LineIO lio = new LineIO(); // get input from stdin
    TokenReader tio = new TokenReader(lio);
    Lazy<Token> lzytok = new Lazy<Token>(tio);
    while(true) {
      Token tok = lzytok.cur();
      if (tok.val == Token.Val.EOF)
        break;
      System.out.printf("%6d: %9s \"%s\"\n",
          tok.lno,
          tok.val,
          tok.str);
      if (tok.val == Token.Val.NEWLINE)
        System.out.println(lio.line);
      lzytok.adv();
    }
  }
}
