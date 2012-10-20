import java.util.*;

/**
* Value determines the value of tokens that have been read in (shocking)
* 
**/
public abstract class Value {

  public Value add(Sax sax, Value v) {
    sax.msg(this + ": cannot appear as first argument to add");
    return UNDEF;
  }

  public Value sub(Sax sax, Value v) {
    sax.msg(this + ": cannot appear as first argument to sub");
    return UNDEF;
  }

  public Value mul(Sax sax, Value v) {
    sax.msg(this + ": cannot appear as first argument to mul");
    return UNDEF;
  }

  public Value div(Sax sax, Value v) {
    sax.msg(this + ": cannot appear as first argument to div");
    return UNDEF;
  }

  public Value mod(Sax sax, Value v) {
    sax.msg(this + ": cannot appear as first argument to mod");
    return UNDEF;
  }

  public Value neg(Sax sax) {
    sax.msg(this + ": cannot appear as argument to neg");
    return UNDEF;
  }

  public int getTag() {
    throw new RuntimeException(this + ": undefined tag value");
    // return -1;
  }

  public int getVal() {
    throw new RuntimeException(this + ": undefined tag value");
    // return -1;
  }

  // if the host Value is Defined (absolute or relative)
  // or Extern, return the host Value, otherwise return UNDEF.
  public Value validate() {
    if (isDefined()) {
      int tag = getTag();
      if (tag == 0 || tag == 1)
        return this;
      else
        return UNDEF;
    } else if (isExtern())
      return this;
    return UNDEF;
  }

  public boolean isAbsolute() {
    return isDefined() && getTag() == 0;
  }

  public boolean isRelative() {
    return isDefined() && getTag() == 1;
  }

  public String toString() {
    return "tag= " + getTag() + ". Value: " + getVal();
  }

  // The following integer-valued strings are recognized
  // by Value.parseInt:
  //   Zero:           0
  //   Hexadecimal:    0xd... where d is a hexadecimal digit (0-9,a-f)
  //   Octal:          0d...  where d is an octal digit (0-7)
  //   Decimal:        1d...  where d is a decimal digit (0-9)
  //   Character:      'x     where x is a printable character
  public static int parseInt(String s) throws NumberFormatException {
    int len = s.length();
    if (s == null || len == 0)
      throw new NumberFormatException("zero-length string");
     /* FIXME */
  
    //it's just a 0, not a octal or hex
    if(s.charAt(0) ==  '0' && len == 1)
    return 0;

  //create a new hex
  if(s.toUpperCase().startsWith("0X"))
    return Integer.parseInt(s.substring(2,len), 16);	

  //create a new octal
  if(s.startsWith("0"))
    return Integer.parseInt(s.substring(1,len), 8);

  //a new char
  if(s.startsWith("\'"))
    return (int)s.charAt(1);

  return Integer.parseInt(s);

  // you may want to use the Integer.parseInt static method
  // to handle the various bases of hex, octal, and decimal (default).
}

public static final Value UNDEF = new Undef(); // a "singleton"

public boolean isDefined() {
  return this instanceof Defined;
}

public boolean isExtern() {
  return this instanceof Extern;
}

public void addFixup(int LC) {
  throw new RuntimeException(this + ": not an EXTERN Value");
}

public static class Defined extends Value {

  private int tag;   // 0=absolute, 1=relocatable
  private int val;   // the value

  public Defined(int tag, int val) {
    this.tag = tag;
    this.val = val;
  }

  // used to construct a Value with absolute tag
  public Defined(int val) {
    this.tag = 0;
    this.val = val;
  }

  public Value add(Sax sax, Value v) {
    if (! v.isDefined()) {
      sax.msg(v+": cannot appear as second argument to add");
      return UNDEF;
    }
    return new Defined(tag + v.getTag(), val + v.getVal());
  }

  /****/
  public Value sub(Sax sax, Value v) {
    /* FIXME */
    //you must not subtract a relative
    if (! v.isDefined()) {
      sax.msg(v+": cannot appear as second argument to subtract");
      return UNDEF;
    }
    return new Defined(tag - v.getTag(), val - v.getVal());
  }

  public Value mul(Sax sax, Value v) {
    /* FIXME */
    // both arguments to the multiply operation
    // must be Defined and absolute
    if(! v.isDefined()) {
      sax.msg(v+": cannot appear as second argument to multiply");
      return UNDEF;
    }
    //ensure that both values are absolute
    if(v.getTag()!=0 || this.getTag() != 0){
      sax.msg("The tags for either " + v + " or " + this + " are not absolute");
      return UNDEF;
    }
    return new Defined(tag, this.val*v.getVal());
  }

  public Value div(Sax sax, Value v) {
    /* FIXME */
    //you must not divide by relatives
    if(! v.isDefined()){
      sax.msg(v+": cannot appear as second argument to divide");
      return UNDEF;
    }
    //check for divide by zero
    if(v.getVal() == 0){
      sax.msg(v+": Cannot divide by zero"); 
    }
    return new Defined(tag, this.val/v.getVal()); 
  }

  public Value mod(Sax sax, Value v) {
    /* FIXME */
    //you must not devide by relatives
    if(! v.isDefined()){
      sax.msg(v+": cannot appear as second argument to modulo");
      return UNDEF;
    }
    //check for divide by zero
    if(v.getVal() == 0){
      sax.msg(v+": Cannot modulo by zero"); 
    }

    return new Defined(tag, this.val%v.getVal()); 

  }

  public Value neg(Sax sax) {
    return new Defined(-tag, -val);
  }

  public int getTag() {
    return tag;
  }

  public int getVal() {
    return val;
  }

  public String toString() {
    return "DEFINED: [tag="+tag+" val="+val+"]";
  }
}

private static class Undef extends Value {
  public String toString() {
    return "UNDEF";
  }
}

public static class Extern extends Value {

  public String label;
  // fixupList gives the object module locations
  // where the EXTERN label is found and must be fixed up
  // at link time
  public List<Integer> fixupList;
  // when fixed up a link time, this offset is added to
  // the addres of the external label
  public int offset;

  // used only when declaring a label to be EXTERN
  public Extern(String label) {
    this.label = label;
    fixupList = new ArrayList<Integer>();
    this.offset = 0;
  }

  // used to instantiate the Value of an EXTERN [+-] abs expression;
  // the ext parameter refers to the EXTERN part of this expression
  // so that this EXTERN can access the label and its fixupList
  private Extern(Extern ext, int offset) {
    // ext is the extern to be operated on
    this.label = ext.label;
    this.fixupList = ext.fixupList;
    this.offset = offset;
  }

  public Value add(Sax sax, Value v) {
    if (! v.isDefined()) {
      sax.msg(v+": cannot appear in expression EXTERN+val");
      return UNDEF;
    }
    if (! v.isAbsolute()) {
      sax.msg(v + ": val non-absolute in expression EXTERN+val");
      return UNDEF;
    }
    return new Extern(this, offset + v.getVal());
  }

  public Value sub(Sax sax, Value v) {
    /* FIXME */
    if(! v.isDefined()){
      sax.msg(v+": cannot appear in expression EXTERN-val");
      return UNDEF;
    }
    if (! v.isAbsolute()) {
      sax.msg(v + ": val non-absolute in expression EXTERN-val");
      return UNDEF;
    }
    return new Extern(this, offset - v.getVal());
  }

  public int getVal() {
    return offset;
  }

  public int getTag() {
    return 0; // implicitly absolute
  }

  public void addFixup(int LC) {
    fixupList.add(LC);
  }

  public String toString() {
    return "EXTERN: [label="+label+" offset="+offset+"]";
  }
}

}
