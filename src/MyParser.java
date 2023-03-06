package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.ast.*;

import java.util.ArrayList;

public class MyParser implements IParser {

    ArrayList<IToken> tokens;
    int currIndex = 0;

    //constructor ---- creates scanner obj from input and gets resulting array of tokens
    public MyParser(String input) throws PLCException {
        MyScanner scanner = (MyScanner) CompilerComponentFactory.makeScanner(input);
        tokens = scanner.getTokenList();
        if(tokens.size() == 0) {
            throw new SyntaxException("EMPTY PROGRAM");
        }
    }

    //where recursive descent is implemented
    @Override
    public AST parse() throws PLCException {
        return expression();
    }

    //<expr> ::= <conditional_expr> | <or_expr>
    Expr expression() throws PLCException {
        if(peek().getKind() == IToken.Kind.RES_if) {
            currIndex++;
            return conditional();
        }
        return orExpr();
    }

    //<conditional_expr> ::= if <expr> ? <expr> ? <expr>
    ConditionalExpr conditional() throws PLCException {
        IToken first = previous();

        Expr guard = expression();
        consume(IToken.Kind.QUESTION, "Question mark expected in conditional");
        Expr t = expression();
        consume(IToken.Kind.QUESTION, "Question mark expected in conditional");
        Expr f = expression();

        return new ConditionalExpr(first, guard, t, f);
    }

    //<or_expr> ::= <and_expr> (  ( | | || ) <and_expr>)*
    Expr orExpr() throws PLCException {
        IToken first = peek();
        Expr expr1 = andExpr();
        while(match(IToken.Kind.BITOR, IToken.Kind.OR)) {
            IToken.Kind currOp = previous().getKind();
            Expr expr2 = andExpr();
            expr1 = new BinaryExpr(first, expr1, currOp, expr2);
        }
        return expr1;
    }

    //<and_expr> ::=  <comparison_expr> ( ( & | && )  <comparison_expr>)*
    Expr andExpr() throws PLCException {
        IToken first = peek();
        Expr expr1 = comparisonExpr();
        while(match(IToken.Kind.BITAND, IToken.Kind.AND)) {
            IToken.Kind currOp = previous().getKind();
            Expr expr2 = comparisonExpr();
            expr1 = new BinaryExpr(first, expr1, currOp, expr2);
        }
        return expr1;
    }

    //<comparison_expr> ::= <power_expr> ( (< | > | == | <= | >=) <power_expr>)*
    Expr comparisonExpr() throws PLCException {
        IToken first = peek();
        Expr expr1 = powerExpr();
        while(match(IToken.Kind.LT, IToken.Kind.GT, IToken.Kind.EQ, IToken.Kind.LE, IToken.Kind.GE)) {
            IToken.Kind currOp = previous().getKind();
            Expr expr2 = powerExpr();
            expr1 = new BinaryExpr(first, expr1,currOp, expr2);
        }
        return expr1;
    }

    //<power_expr> ::=  <additive_expr> [(**(power_expr)) | empty_set]
    Expr powerExpr() throws PLCException {
        IToken first = peek();
        Expr expr2 = additiveExpr();

        if(match(IToken.Kind.EXP)) {
            Expr expr1 = powerExpr();
            expr2 = new BinaryExpr(first, expr2, IToken.Kind.EXP, expr1);
        }
        return expr2;
    }

    //<additive_expr> ::= <multiplicative_expr> ( ( + | - ) <multiplicative_expr> )*
    Expr additiveExpr() throws PLCException {
        IToken first = peek();
        Expr expr1 = multiplicativeExpr();
        while(match(IToken.Kind.PLUS, IToken.Kind.MINUS)) {
            IToken.Kind currOp = previous().getKind();
            Expr expr2 = multiplicativeExpr();
            expr1 = new BinaryExpr(first, expr1, currOp, expr2);
        }
        return expr1;
    }

    //<multiplicative_expr> ::= <unary_expr> (( * | / | % ) <unary_expr>)*
    Expr multiplicativeExpr() throws PLCException {
        IToken first = peek();
        Expr expr1 = unaryExpr();
        while(match(IToken.Kind.TIMES, IToken.Kind.DIV, IToken.Kind.MOD)) {
            IToken.Kind currOp = previous().getKind();
            Expr expr2 = unaryExpr();
            expr1 = new BinaryExpr(first, expr1, currOp, expr2);
        }
        return expr1;
    }

    //<unary_expr> ::= ( ! | - | sin | cos | atan) <unary_expr> | <primary_expr>
    Expr unaryExpr() throws PLCException {
        IToken first = peek();
        if(match(IToken.Kind.BANG, IToken.Kind.MINUS, IToken.Kind.RES_sin, IToken.Kind.RES_cos, IToken.Kind.RES_atan)) {
            IToken operator = previous();
            Expr right = unaryExpr();
            return new UnaryExpr(first, operator.getKind(), right);
        }
        return primaryExpr();
    }

    //<primary_expr> ::= STRING_LIT | NUM_LIT | IDENT | ( <expr> ) | Z | rand
    Expr primaryExpr() throws PLCException {
        IToken first = peek();
        IToken.Kind k = first.getKind();

        currIndex++;
        if(k == IToken.Kind.STRING_LIT) {
            return new StringLitExpr(first);
        }
        else if (k == IToken.Kind.NUM_LIT) {
            return new NumLitExpr(first);
        }
        else if (k == IToken.Kind.IDENT) {
            return new IdentExpr(first);
        }
        else if (k == IToken.Kind.RES_Z) {
            return new ZExpr(first);
        }
        else if (k == IToken.Kind.RES_rand) {
            return new RandomExpr(first);
        }
        else if (k == IToken.Kind.LPAREN) {
            Expr expr = expression();
            consume(IToken.Kind.RPAREN, "Right parenthesis expected");
            return expr;
        }
        else if (k == IToken.Kind.ERROR) {
            throw new LexicalException("Invalid token");
        }
        throw new SyntaxException("Unexpected token");
    }


    //================ UTILITY FUNCTIONS ==================//

    //determines whether current token type is in the provided set of token types
    boolean match(IToken.Kind ... kinds) throws PLCException {
        if(currIndex > tokens.size() - 1) {
            return false;
        }
        for(IToken.Kind k : kinds) {
            if(check(k)) {
                advance();
                return true;
            }
        }
        return false;
    }

    //checks whether current token type matches the provided type
    boolean check(IToken.Kind k) throws PLCException {
        if(currIndex > tokens.size() - 1) {
            return false;
        }
        return peek().getKind() == k;
    }

    IToken consume(IToken.Kind k, String message) throws PLCException {
        if (currIndex > tokens.size() - 1){
            throw new SyntaxException(message);
        }
        if (check(k)) {
            return advance();
        }
        throw new SyntaxException(message);
    }

    //increments the current index if end of list has not been reached
    IToken advance() {
        if(currIndex <= tokens.size() - 1) {
            currIndex++;
        }
        return previous();
    }

    //gets the token at the previous index
    IToken previous() {
        if(currIndex == 0) {
            return null;
        }
        return tokens.get(currIndex - 1);
    }

    //gets the token at the current index
    IToken peek() throws PLCException {
        if(currIndex > tokens.size() - 1) {
            throw new SyntaxException("bad");
        }
        IToken currToken = tokens.get(currIndex);
        if (currToken.getKind() == IToken.Kind.ERROR){
            throw new LexicalException(currToken.getTokenString());
        }
        return currToken;
    }

}