package edu.ufl.cise.plcsp23;

import java.util.*;

import static java.lang.Character.*;

//import static java.lang.Character.isDigit;

public class MyScanner implements IScanner {
    ArrayList<IToken> list_of_tokens;
    int currIndex = 0;

    public static HashMap<String, IToken.Kind> reservedWords = new HashMap<String, IToken.Kind>() {{
        put("image", IToken.Kind.RES_image);
        put("pixel", IToken.Kind.RES_pixel);
        put("int", IToken.Kind.RES_int);
        put("string", IToken.Kind.RES_string);
        put("void", IToken.Kind.RES_void);
        put("nil", IToken.Kind.RES_nil);
        put("load", IToken.Kind.RES_load);
        put("display", IToken.Kind.RES_display);
        put("write", IToken.Kind.RES_write);
        put("x", IToken.Kind.RES_x);
        put("y", IToken.Kind.RES_y);
        put("a", IToken.Kind.RES_a);
        put("r", IToken.Kind.RES_r);
        put("X", IToken.Kind.RES_X);
        put("Y", IToken.Kind.RES_Y);
        put("Z", IToken.Kind.RES_Z);
        put("x_cart", IToken.Kind.RES_x_cart);
        put("y_cart", IToken.Kind.RES_y_cart);
        put("a_polar", IToken.Kind.RES_a_polar);
        put("r_polar", IToken.Kind.RES_r_polar);
        put("rand", IToken.Kind.RES_rand);
        put("sin", IToken.Kind.RES_sin);
        put("cos", IToken.Kind.RES_cos);
        put("atan", IToken.Kind.RES_atan);
        put("if", IToken.Kind.RES_if);
        put("while", IToken.Kind.RES_while);
    }};

    public static HashMap<String, IToken.Kind> opSingleChar = new HashMap<String, IToken.Kind>() {{
        put(".", IToken.Kind.DOT);
        put(",", IToken.Kind.COMMA);
        put("?", IToken.Kind.QUESTION);
        put(":", IToken.Kind.COLON);
        put("(", IToken.Kind.LPAREN);
        put(")", IToken.Kind.RPAREN);
        put("[", IToken.Kind.LSQUARE);
        put("]", IToken.Kind.RSQUARE);
        put("{", IToken.Kind.LCURLY);
        put("}", IToken.Kind.RCURLY);
        put("!", IToken.Kind.BANG);
        put("+", IToken.Kind.PLUS);
        put("-", IToken.Kind.MINUS);
        put("/", IToken.Kind.DIV);
        put("%", IToken.Kind.MOD);
    }};

    public static HashMap<String, IToken.Kind> opInitial = new HashMap<String, IToken.Kind>() {{
        put("<",  IToken.Kind.LT);
        put(">", IToken.Kind.GT);
        put("=", IToken.Kind.ASSIGN);
        put("&", IToken.Kind.BITAND);
        put("|", IToken.Kind.BITOR);
        put("*", IToken.Kind.TIMES);
    }};

    public static HashMap<String, IToken.Kind> opMultiChar = new HashMap<String, IToken.Kind>() {{
        put("==", IToken.Kind.EQ);
        put("<->", IToken.Kind.EXCHANGE);
        put("<=", IToken.Kind.LE);
        put(">=", IToken.Kind.GE);
        put("&&", IToken.Kind.AND);
        put("||", IToken.Kind.OR);
        put("**", IToken.Kind.EXP);
    }};

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
                        }
                    }
                    //else if (a-z, A-Z, _) state = ident
                    else if (isAlphabetic(ch) | ch == '_') {
                        state = State.IN_IDENT;
                        currToken += ch;
                    }
                    //else if (~) state = COMMENT
                    else if (ch == '~'){
                        state = State.COMMENT;
                        currToken += ch;
                    }
                    else if (ch == '"'){
                        state = State.IN_STRING_LIT;
                        currToken += ch;
                    }
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
                        if (reservedWords.containsKey(currToken)){
                            tokens.add(new MyToken(currToken, reservedWords.get(currToken), new IToken.SourceLocation(row, col)));
                        }
                        else {
                            tokens.add(new MyToken(currToken, IToken.Kind.IDENT, new IToken.SourceLocation(row, col)));
                        }
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
                if (reservedWords.containsKey(currToken)){
                    tokens.add(new MyToken(currToken, reservedWords.get(currToken), new IToken.SourceLocation(row, col)));
                }
                else {
                    tokens.add(new MyToken(currToken, IToken.Kind.IDENT, new IToken.SourceLocation(row, col)));
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


        if(list_of_tokens.get(currIndex).getKind().equals(IToken.Kind.ERROR)){
            throw new LexicalException("NUM TOO LRG");
        }
        //get current token
        IToken ret = list_of_tokens.get(currIndex);
        currIndex++;
        return ret;
    }

}
