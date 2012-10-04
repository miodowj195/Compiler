package cis463.fsm;

public interface FSMState {

    // When in this state, calling next() will examine the
    // current information known to this state
    // then will return the next state in the FSM or null
    // if it is the stop state
    public FSMState next();

}
