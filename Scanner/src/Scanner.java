import cis463.util.*;
import java.util.*;

public class Scanner {

  //create a map so we can build a cross reference table
	private static Map<String, List<Integer>> xRefTab;

  public static void main(String [] args) {
		//use a treemap so sorting isn't an issue
    xRefTab = new TreeMap<String, List<Integer>>();
    LineIO lio = new LineIO(); // get input from stdin
    TokenReader tio = new TokenReader(lio);
    Lazy<Token> lzytok = new Lazy<Token>(tio);
    

		while(true) {
      Token tok = lzytok.cur();
			//only add tokens that are IDs to xRefTab
      if(tok.val == Token.Val.ID)
        addToXRefTab(tok, xRefTab);

			//print out the tokens are per the assingment description
      System.out.printf("%6d: %9s \"%s\"\n", tok.lno, tok.val, tok.str);
      //done if we hit EOF
			if (tok.val == Token.Val.EOF)
        break;
			//otherwise continue on ... 
      lzytok.adv();
    }

		//only create the xRefTab if there are IDs to print
    if(xRefTab.size() != 0)
      xTabCreate();
  }

	//method that allows for tokens to be added to the xRefTab
  private static void addToXRefTab(Token tok, Map<String, List<Integer>> xRefTab){
    //Check to see if the ID is already in the set of keys
    if(!xRefTab.containsKey(tok.str.toString())){
			//if not, create a new entry for it and an associated list
      List<Integer> l = new ArrayList<Integer>();
      l.add(tok.lno);
      xRefTab.put(tok.str.toString(),l);
      return;
    }

    //if this ID has already occured on this line, do nothing
    if(xRefTab.get(tok.str.toString()).contains(tok.lno))
      return;

		//otherwise add the line number to the associated key
    xRefTab.get(tok.str.toString()).add(tok.lno);
    return;
  }

	//build the xRefTab for output
  private static void xTabCreate(){
    int dig;
		//print the header...
    System.out.println("\nIdentifier Cross Reference Table\n--------------------------------");
    //format the output so that it conforms to the spec in the assignment description
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
