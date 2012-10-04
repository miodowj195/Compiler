import java.util.*;

// This class is used to keep track of the list of line numbers
// in which this label appears.  In its present form, it does not
// identify the line number where the identifier is defined (in
// label position or as a declared EXTERN)
public class Xref {

    public Stack<Integer> xrefList;

    public Xref() {
	xrefList = new Stack<Integer>();
    }

    // add n to the cross-reference list
    public void addXref(int n) {
	if (xrefList.size() == 0 || xrefList.peek() != n)
	    xrefList.push(n);
    }

    public Stack<Integer> getXrefList() {
	return xrefList;
    }

}
