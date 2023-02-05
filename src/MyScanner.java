package edu.ufl.cise.plcsp23;

import java.util.ArrayList;
import static java.lang.Character.*;

//import static java.lang.Character.isDigit;

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
        ERROR,
        COMMENT
    }

    public MyScanner(String input) throws LexicalException {
        this.list_of_tokens = Tokenize(input);
    }

    public ArrayList<IToken> Tokenize(String scannerInput) throws LexicalException {
        ArrayList<IToken> tokens = new ArrayList<IToken>();
        State state = State.START;
        String currToken = "";

        //increment when new line is encountered
        int row = 1;
        //set to 1 when new line is encountered
        int col = 1;

        //if empty string, return EOF token
        int length = scannerInput.length();
        if(length == 0) {
            return tokens;
        }

        forloop:
        for(int i = 0; i < length; i++) {
            char ch = scannerInput.charAt(i);
            switch(state) {
                case START -> {
                    //if whitespace, continue no change
                    if(ch == 32 | ch == 13 | ch == 10 | ch == 12 | ch == 9) {
                        state = State.START;
                    }

                    //else if 0, add token to list and keep START state

                    //else if (1-9), state = num_lit
                    else if(isDigit(ch)){
                        if(ch == '0') {
                            //add to list
                            tokens.add(new NumLitToken(0, new IToken.SourceLocation(row,col), IToken.Kind.NUM_LIT, "0"));
                        }
                        else {
                            state = State.IN_NUM_LIT;
                            currToken += ch;

                            //CHECK SIZE OF INT

                        }
                    }
                    //else if (a-z, A-Z, _) state = ident
                    else if (isAlphabetic(ch) | ch == '_') {
                        state = State.IN_IDENT;
                        currToken += ch;
                    }
                    //else if (~) state = COMMENT
                    //else if (") state = string_lit
                    //else if operator --> one two or three chars?
                    //else error or eof ??
                }



                case IN_NUM_LIT -> {
                    //if (0-9), state remain num_lit, add to currToken
                    //else; add new token to list, set currToken empty, set state to START, i--
                    if(isDigit(ch)){
                        currToken += ch;
                        long lengthTest = Long.parseLong(currToken);
                        if (lengthTest > Integer.MAX_VALUE){
                            tokens.add(new MyToken("Number is larger than INT MAX", IToken.Kind.ERROR, new IToken.SourceLocation(row, col)));
                            state = State.ERROR;
                            break forloop;
                        }
                    }
                    else {
                        tokens.add(new NumLitToken(Integer.parseInt(currToken), new IToken.SourceLocation(row,col), IToken.Kind.NUM_LIT, currToken));
                        state = State.START;
                        currToken = "";
                        i--;
                    }
                }




                case IN_IDENT -> {
                    //if (letter | _ | number), state remains ident, add to currToken
                    //else; add new token to list, set currToken empty, set state to START, i--
                    if (isAlphabetic(ch) | isDigit(ch) | ch == '_'){
                        currToken += ch;
                    }
                    else {
                        tokens.add(new MyToken(currToken, IToken.Kind.IDENT, new IToken.SourceLocation(row, col)));
                        state = State.START;
                        currToken = "";
                        i--;
                    }
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

        switch(state){
            case IN_NUM_LIT -> {
                try {
                    tokens.add(new NumLitToken(Integer.parseInt(currToken), new IToken.SourceLocation(row,col), IToken.Kind.NUM_LIT, currToken));
                }
                catch(NumberFormatException e) {
                    throw new LexicalException("Number is too Large");
                }
            }
            case IN_IDENT -> {
                tokens.add(new MyToken(currToken, IToken.Kind.IDENT, new IToken.SourceLocation(row, col)));
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


        if(list_of_tokens.get(currIndex).getKind().equals(IToken.Kind.ERROR)){
            throw new LexicalException("NUM TOO LRG");
        }
        //get current token
        IToken ret = list_of_tokens.get(currIndex);
        currIndex++;
        return ret;
    }

}
