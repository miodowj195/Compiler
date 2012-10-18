import cis463.util.*;
import cis463.fsm.*;
import java.util.*;

public class Eval {

  private Lazy<Token> lztok;
  private Stack<Value> stk;
  public Sax sax; // the FSMStates can see this 

  public Eval(Sax sax) {
    this.lztok = sax.lztok;
    this.stk = new Stack<Value>();
    this.sax = sax;
  }

  private void debug(String s) {
    // System.err.println(s);
  }

  public Value eval() {
    FSM.run(expr_EXPR);
    Value v = stk.pop().validate();
    // System.err.println("... expression value = " + v + " ...");
    return v;
  }

  public FSMState expr_EXPR = new FSMState() {
    public FSMState next() {
      FSM.run(expr_TERM);
      return expr_EXPR1;
    }
  };

  public FSMState expr_EXPR1 = new FSMState() {
    public FSMState next() {
      Token t = CUR();
      switch(t.val) {
        case PLUS: {
                     ADV();
                     Value x = stk.pop();
                     FSM.run(expr_TERM);
                     stk.push(x.add(sax, stk.pop()));
                     return this;
                   }
        case MINUS: {
                      ADV();
                      /* FIXME */
                      Value x = stk.pop();
                      FSM.run(expr_TERM);
                      stk.push(x.sub(sax, stk.pop()));
                      return this;
                    }
        default:
                    return null;
      }
    }
  };

  public FSMState expr_TERM = new FSMState() {
    public FSMState next() {
      debug("TERM");
      FSM.run(expr_FACTOR);
      return expr_TERM1;
    }
  };

  public FSMState expr_TERM1 = new FSMState() {
    public FSMState next() {
      Token t = CUR();
      debug("TERM1: " + t);
      switch(t.val) {
        case TIMES: {
                      ADV();
                      Value x = stk.pop();
                      FSM.run(expr_FACTOR);
                      stk.push(x.mul(sax, stk.pop()));
                      return this;
                    }
        case DIVIDE: {
                       ADV();
                       /* FIXME */
                       Value x = stk.pop();
                       FSM.run(expr_FACTOR);
                       stk.push(x.div(sax, stk.pop()));
                       return this;
                     }
        case MOD: {
                    ADV();
                    /* FIXME */
                    Value x = stk.pop();
                    FSM.run(expr_FACTOR);
                    stk.push(x.mod(sax, stk.pop()));
                    return this;
                  }
        default:
                  return null;
      }
    }
  };

  public FSMState expr_FACTOR = new FSMState() {
    public FSMState next() {
      Token t = CUR();
      debug("FACTOR: " + t);
      switch(t.val) {
        case DOLLAR:
          ADV();
          stk.push(new Value.Defined(1, sax.LC));
          return null;
        case INT:
          ADV();
          try {
            int v = Value.parseInt(t.str.toString());
            stk.push(new Value.Defined(v)); // it's absolute!
            return null;
          } catch (NumberFormatException e) {
            stk.push(new Value.Defined(0)); // overflow!
            /* FIXME?? throw new RuntimeException(e); */
          }
        case MINUS:
          // unary minus
          ADV();
          FSM.run(expr_FACTOR);
          stk.push(stk.pop().neg(sax));
          return null;
        case LPAREN:
          ADV();
          // recursively get the value of the inner expression
          FSM.run(expr_EXPR);
          return expr_PAREN;
        case ID:
          ADV();
          String id = t.str.toString();
          Value val = sax.symtabGet(id);
          stk.push(val);
          return null;
        default:
          sax.err(t + ": illegal token in expression");
          throw new RuntimeException("illegal token: " + t);
      }
    }
  };

  public FSMState expr_PAREN = new FSMState() {
    public FSMState next() {
      Token t = CUR();
      debug("PAREN: " + t);
      switch (t.val) {
        case RPAREN:
          ADV();
          return null;
        default:
          sax.err("missing right parentheses in expression");
          throw new RuntimeException("missing right paren");
      }
    }
  };

  public Token CUR() {
    return lztok.cur();
  }

  public void ADV() {
    lztok.adv();
  }
}
