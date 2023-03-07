package edu.ufl.cise.plcsp23;

import edu.ufl.cise.plcsp23.ast.*;

import java.beans.Expression;
import java.util.ArrayList;
import java.util.List;

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
        return Program();
    }

    //<program> ::= <type> IDENT (ParamList) Block
    AST Program() throws PLCException {
        IToken first = advance();
        try {
            Type.getType(first);
        }
        catch(RuntimeException r) {
            throw new SyntaxException("Invalid Type");
        }
        Ident id = new Ident(consume(IToken.Kind.IDENT, "Ident expected"));

        consume(IToken.Kind.LPAREN, "Left parentheses expected");
        List<NameDef> params = null;
        //TODO========== Return list of name def arguments
        consume(IToken.Kind.RPAREN, "Right parentheses expected");

        Block block = Block();

        return new Program(first, Type.getType(first), id, params, block);
    }

    //<Block> ::= {DecList StatementList}
    Block Block() throws PLCException {
        IToken first = peek();

        consume(IToken.Kind.LCURLY, "Left curly expected");
        List<Declaration> decList = null;
        List<Statement> statementList = null;
        consume(IToken.Kind.RCURLY, "Right curly expected");

        return new Block(first, decList, statementList);
    }

    //<DecList> ::= (Declaration.)*
    AST DecList() throws PLCException
    {
        return null;
    }

    //<StatementList> ::= (Statement.)*
    AST StatementList() throws PLCException
    {
        return null;
    }

    //<ParamList> ::= e | NameDef (,NameDef)*
    AST ParamList() throws PLCException {
        return null;
    }

    //<NameDef> ::= Type (IDENT | Dimension IDENT)
    NameDef NameDef() throws PLCException
    {
        IToken first = advance();
        Dimension dimension = null;
        Ident id = null;
        if(match(IToken.Kind.IDENT)) {
            id = new Ident(previous());
        }
        else {
            dimension = Dimension();
            id = new Ident(consume(IToken.Kind.IDENT, "Ident expected"));
        }
        return new NameDef(first, Type.getType(first), dimension, id);
    }


    //<Declaration> ::= NameDef (e | = Expr)
    AST Declaration() throws PLCException
    {
        IToken first = peek();
        NameDef name = NameDef();
        Expr exp = null;
        if(match(IToken.Kind.ASSIGN)) {
            exp = expression();
        }
        return new Declaration(first, name, exp);
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

    //<unary_expr> ::= ( ! | - | sin | cos | atan) <unary_expr> | <unaryExprPostfix>
    Expr unaryExpr() throws PLCException {
        IToken first = peek();
        if(match(IToken.Kind.BANG, IToken.Kind.MINUS, IToken.Kind.RES_sin, IToken.Kind.RES_cos, IToken.Kind.RES_atan)) {
            IToken operator = previous();
            Expr right = unaryExpr();
            return new UnaryExpr(first, operator.getKind(), right);
        }
        return primaryExpr();
    }

    //<UnaryExprPostfix> ::= <primaryExpr> (<PixelSelector> | e)(<ChannelSelector> | e)
    Expr UnaryExprPostfix() throws PLCException
    {
        IToken first = peek();
        Expr primExp = primaryExpr();
        PixelSelector pix = null;
        if(match(IToken.Kind.LSQUARE)) {
            pix = PixelSelector();
        }
        IToken color = null;
        if(match(IToken.Kind.COLON)) {
            color = peek();
        }
        return new UnaryExprPostfix(first, primExp, pix, ColorChannel.getColor(color));
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
        else if (k == IToken.Kind.RES_x || k == IToken.Kind.RES_y || k == IToken.Kind.RES_a || k == IToken.Kind.RES_r) {
            return new PredeclaredVarExpr(first);
        }
        else if (k == IToken.Kind.RES_rand) {
            return new RandomExpr(first);
        }
        else if (k == IToken.Kind.LPAREN) {
            Expr expr = expression();
            consume(IToken.Kind.RPAREN, "Right parenthesis expected");
            return expr;
        }
        else if (k == IToken.Kind.LSQUARE) {
            return ExpandedPixel();
        }
        else if (k == IToken.Kind.RES_x_cart || k == IToken.Kind.RES_y_cart || k == IToken.Kind.RES_a_polar || k == IToken.Kind.RES_r_polar){
            return PixelFunctionExpr();
        }
        else if (k == IToken.Kind.ERROR) {
            throw new LexicalException("Invalid token");
        }
        throw new SyntaxException("Unexpected token");
    }


    //PixelSelector ::= [ Expr , Expr ]
    PixelSelector PixelSelector() throws PLCException
    {
        IToken first = advance();
        Expr expr1 = expression();
        consume(IToken.Kind.COMMA, "Comma expected");
        Expr expr2 = expression();
        consume(IToken.Kind.RSQUARE, "Right square expected");
        return new PixelSelector(first, expr1, expr2);
    }

    //ExpandedPixel ::= [ Expr, Expr, Expr ]
    Expr ExpandedPixel() throws PLCException {
        IToken first = previous();
        Expr expr1 = expression();
        consume(IToken.Kind.COMMA, "Comma expected");
        Expr expr2 = expression();
        consume(IToken.Kind.COMMA, "Comma expected");
        Expr expr3 = expression();
        consume(IToken.Kind.RSQUARE, "Right square expected");
        return new ExpandedPixelExpr(first, expr1, expr2, expr3);
    }

    //PixelFunctionExpr ::= (x_cart | y_cart | a_polar | r_polar) PixelSelector
    Expr PixelFunctionExpr() throws PLCException
    {
        IToken first = previous();
        PixelSelector pixSelect = PixelSelector();
        return new PixelFuncExpr(first, first.getKind(), pixSelect);
    }

    //Dimension ::= [ Expr, Expr ]
    Dimension Dimension() throws PLCException
    {
        IToken first = advance();
        Expr expr1 = expression();
        consume(IToken.Kind.COMMA, "Comma expected");
        Expr expr2 = expression();
        consume(IToken.Kind.RSQUARE, "Right square expected");
        return new Dimension(first, expr1, expr2);
    }

    //LValue ::= IDENT (PixelSelector | e) (ChannelSelector | e)
    LValue LValue() throws PLCException
    {
        IToken first = peek();
        Ident id = new Ident(consume(IToken.Kind.IDENT, "Ident expected"));

        PixelSelector pix = null;
        if(match(IToken.Kind.LSQUARE)) {
            pix = PixelSelector();
        }
        IToken color = null;
        if(match(IToken.Kind.COLON)) {
            color = peek();
        }
        return new LValue(first, id, pix, ColorChannel.getColor(color));
    }

    // Statement ::= LValue = Expr | write Expr | while Expr Block
    AST Statement() throws PLCException
    {
        IToken first = peek();
        if(match(IToken.Kind.RES_write)){
            Expr exp = expression();
            return new WriteStatement(first, exp);
        }
        else if(match(IToken.Kind.RES_while)) {
            Expr exp = expression();
            Block block = Block();
            return new WhileStatement(first, exp, block);
        }
        else {
            LValue Lval = LValue();
            consume(IToken.Kind.ASSIGN, "Assignment operator expected");
            Expr exp = expression();
            return new AssignmentStatement(first, Lval, exp);
        }
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