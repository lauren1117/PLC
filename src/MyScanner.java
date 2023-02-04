package edu.ufl.cise.plcsp23;

import java.util.ArrayList;

public class MyScanner implements IScanner {
    ArrayList<IToken> list_of_tokens;
    int currIndex = 0;

    private enum State {
        //adjust with our own states
        START,
        IN_IDENT,
        IN_NUM_LIT,
        IN_STRING_LIT,
        ESCAPE,
        HAVE_EQ,
        COMMENT
    }

    public MyScanner(String input) {
        this.list_of_tokens = Tokenize(input);
    }

    public ArrayList<IToken> Tokenize(String scannerInput) {
        ArrayList<IToken> tokens = new ArrayList<IToken>();
        State state = State.START;

        //increment when new line is encountered
        int row = 1;
        //set to 1 when new line is encountered
        int col = 1;

        //if empty string, return EOF token
        int length = scannerInput.length();
        if(length == 0) {
            return tokens;
        }

        for(int i = 0; i < length; i++) {
            char ch = scannerInput.charAt(i);
            switch(state) {
                case START -> {
                    //if whitespace, continue no change
                    if(ch == 32 | ch == 13 | ch == 10 | ch == 12 | ch == 9) {
                        //update row, col somehow ??
                        continue;
                    }

                    //else if 0, add token to list and keep START state
                    else if(ch == '0') {}

                    //else if (1-9), state = num_lit
                    //else if (~) state = COMMENT
                    //else if (") state = string_lit
                    //else if (a-z, A-Z, _) state = ident
                    //else if operator --> one two or three chars?
                    //else error or eof ??
                }
                case IN_NUM_LIT -> {
                    //if (0-9), state remain num_lit, add to currToken
                    //else; add new token to list, set currToken empty, set state to START, i--
                }
                case IN_IDENT -> {
                    //if (letter | _ | number), state remains ident, add to currToken
                    //else; add new token to list, set currToken empty, set state to START, i--
                }
                case IN_STRING_LIT -> {
                    //if input char (excluding " or \) state = string_lit
                    // if esc sequence, add to currToken, state = esc
                    //if ", state = start, add toCurrToken, push to list, set string empty
                    //else, error
                }
                case COMMENT -> {
                    //if ascii char except lf cr
                }
                case ESCAPE -> {
                    //if (b | t | n | r | " | \), what do we do????
                    //else, error
                }
            }
        }

        //return token list
        return tokens;
    }

    @Override
    public IToken next() throws LexicalException {
        if(currIndex >= list_of_tokens.size()) {
            //return new EOF token
            //CHANGE SOURCE LOCATION TO CORRECT
            return new MyToken("EOF", IToken.Kind.EOF, new IToken.SourceLocation(0,0));
        }
        //get current token
        IToken ret = list_of_tokens.get(currIndex);
        currIndex++;
        return ret;
    }

}
