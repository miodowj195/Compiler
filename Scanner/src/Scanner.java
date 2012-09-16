import cis463.util.*;
import java.util.*;

public class Scanner {

  private static Map<String, List<Integer>> xRefTab;

  public static void main(String [] args) {
    xRefTab = new TreeMap<String, List<Integer>>();
    LineIO lio = new LineIO(); // get input from stdin
    TokenReader tio = new TokenReader(lio);
    Lazy<Token> lzytok = new Lazy<Token>(tio);
    while(true) {
      Token tok = lzytok.cur();

      if(tok.val == Token.Val.ID)
        addToXRefTab(tok, xRefTab);

      System.out.printf("%6d: %9s \"%s\"\n", tok.lno, tok.val, tok.str);
      if (tok.val == Token.Val.EOF)
        break;
      lzytok.adv();
    }

    if(xRefTab.size() != 0)
      xTabCreate();
  }

  private static void addToXRefTab(Token tok, Map<String, List<Integer>> xRefTab){
    //Check to see if the ID is already in the set of keys
    if(!xRefTab.containsKey(tok.str.toString())){
      List<Integer> l = new ArrayList<Integer>();
      l.add(tok.lno);
      xRefTab.put(tok.str.toString(),l);
      return;
    }

    //if this ID has already occured on this line, do nothing
    if(xRefTab.get(tok.str.toString()).contains(tok.lno))
      return;

    xRefTab.get(tok.str.toString()).add(tok.lno);
    return;
  }

  private static void xTabCreate(){
    int dig;
    System.out.println("\nIdentifier Cross Reference Table\n--------------------------------");
    for(String key : xRefTab.keySet()){
      if(key.length() < 30)
        System.out.printf("%-30s", key);
      else
        System.out.printf("%.29s+", key);

      dig = 0;
      for(Integer i : xRefTab.get(key)){
          if(dig%7 == 0 & dig++ != 0)
            System.out.printf("\n%30s","" );
          
          System.out.printf(" %5d", i); 
      }
      System.out.println("");
    }    
  }


}
