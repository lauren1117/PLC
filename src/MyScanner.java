package edu.ufl.cise.plcsp23;

import java.util.ArrayList;

public class MyScanner implements IScanner {
    int pos;
    char ch;

    ArrayList<IToken> list_of_tokens;

    public MyScanner(String input) {
        list_of_tokens = Tokenize(input);
    }

    public ArrayList<IToken> Tokenize(String scannerInput) {
        pos = 0;
        ch = scannerInput.charAt(pos);
        //actually tokenize
        //switch cases for states, what happens for each
        //check for eof

        return null;
    }

    @Override
    public IToken next() throws LexicalException {
        //what is going on here?????
        return null;
    }

    private enum State {
        //adjust with our own states

        START,
        IN_IDENT,
        IN_NUM_LIT,
        HAVE_EQ
    }

}
