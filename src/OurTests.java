/*Copyright 2023 by Beverly A Sanders
 *
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the
 * University of Florida during the spring semester 2023 as part of the course project.
 *
 * No other use is authorized.
 *
 * This code may not be posted on a public web site either during or after the course.
 */

package edu.ufl.cise.plcsp23;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import edu.ufl.cise.plcsp23.IToken.Kind;
import edu.ufl.cise.plcsp23.IToken.SourceLocation;

class OurTests {

    // makes it easy to turn output on and off (and less typing than
    // System.out.println)
    static final boolean VERBOSE = true;

    void show(Object obj) {
        if (VERBOSE) {
            System.out.println(obj);
        }
    }

    // check that this token has the expected kind
    void checkToken(Kind expectedKind, IToken t) {
        assertEquals(expectedKind, t.getKind());
    }

    void checkToken(Kind expectedKind, String expectedChars, SourceLocation expectedLocation, IToken t) {
        assertEquals(expectedKind, t.getKind());
        assertEquals(expectedChars, t.getTokenString());
        assertEquals(expectedLocation, t.getSourceLocation());
        ;
    }

    void checkIdent(String expectedChars, IToken t) {
        checkToken(Kind.IDENT, t);
        assertEquals(expectedChars.intern(), t.getTokenString().intern());
        ;
    }

    void checkString(String expectedValue, IToken t) {
        assertTrue(t instanceof IStringLitToken);
        assertEquals(expectedValue, ((IStringLitToken) t).getValue());
    }

    void checkString(String expectedChars, String expectedValue, SourceLocation expectedLocation, IToken t) {
        assertTrue(t instanceof IStringLitToken);
        assertEquals(expectedValue, ((IStringLitToken) t).getValue());
        assertEquals(expectedChars, t.getTokenString());
        assertEquals(expectedLocation, t.getSourceLocation());
    }

    void checkNUM_LIT(int expectedValue, IToken t) {
        checkToken(Kind.NUM_LIT, t);
        int value = ((INumLitToken) t).getValue();
        assertEquals(expectedValue, value);
    }

    void checkNUM_LIT(int expectedValue, SourceLocation expectedLocation, IToken t) {
        checkToken(Kind.NUM_LIT, t);
        int value = ((INumLitToken) t).getValue();
        assertEquals(expectedValue, value);
        assertEquals(expectedLocation, t.getSourceLocation());
    }

    void checkTokens(IScanner s, IToken.Kind... kinds) throws LexicalException {
        for (IToken.Kind kind : kinds) {
            checkToken(kind, s.next());
        }
    }

    void checkTokens(String input, IToken.Kind... kinds) throws LexicalException {
        IScanner s = CompilerComponentFactory.makeScanner(input);
        for (IToken.Kind kind : kinds) {
            checkToken(kind, s.next());
        }
    }

    // check that this token is the EOF token
    void checkEOF(IToken t) {
        checkToken(Kind.EOF, t);
    }


    //========================= TESTS BEGIN =============================
    @Test
    void checkReservedAndIdents() throws LexicalException {
        String input = """
				x yx imagethree
				pixel
				sins
				""";
        IScanner scanner = CompilerComponentFactory.makeScanner(input);
        checkToken(Kind.RES_x, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.RES_pixel, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkEOF(scanner.next());
    }

    @Test
    void testNumLit() throws LexicalException {
        String input = "12 30 39854 0345";
        IScanner scanner = CompilerComponentFactory.makeScanner(input);
        checkNUM_LIT(12, scanner.next());
        checkNUM_LIT(30, scanner.next());
        checkNUM_LIT(39854, scanner.next());
        checkNUM_LIT(0, scanner.next());
        checkNUM_LIT(345, scanner.next());
        checkEOF(scanner.next());
    }

    @Test
    void testIdent() throws LexicalException{
        String input = "_019abc _tre";
        IScanner scanner = CompilerComponentFactory.makeScanner(input);
        checkIdent("_019abc", scanner.next());
        checkIdent("_tre", scanner.next());
        checkEOF(scanner.next());
    }

    @Test
    void emptyStringLit() throws LexicalException {
        String input = """
				""
				""";
        IScanner scanner = CompilerComponentFactory.makeScanner(input);
        checkToken(Kind.STRING_LIT, scanner.next());
        checkEOF(scanner.next());
    }

    @Test
    void LTGTEXCHANGE() throws LexicalException {
        String input = """
				<=>>><->><<=<<->
				""";
        checkTokens(input, Kind.LE, Kind.GT, Kind.GT, Kind.GT, Kind.EXCHANGE, Kind.GT,Kind.LT,Kind.LE, Kind.LT,Kind.EXCHANGE);
    }

    @Test
    void allOperatorsAndSeparators() throws LexicalException {
		/*  Operators and Separators . | , | ? | : | ( | ) | < | > | [ | ] | { | } | = | == | <-> | <= |  >= | ! | & | && | | | || |
      + | - | * | ** | / | %   */
        String input = """
				. , ? : ( ) < > [ ] { } = == <-> <= >= ! & && | || + - * ** / %
				""";
        IScanner scanner = CompilerComponentFactory.makeScanner(input);
        checkToken(Kind.DOT, scanner.next());
        checkToken(Kind.COMMA, scanner.next());
        checkToken(Kind.QUESTION, scanner.next());
        checkToken(Kind.COLON, scanner.next());
        checkToken(Kind.LPAREN, scanner.next());
        checkToken(Kind.RPAREN, scanner.next());
        checkToken(Kind.LT, scanner.next());
        checkToken(Kind.GT, scanner.next());
        checkToken(Kind.LSQUARE, scanner.next());
        checkToken(Kind.RSQUARE, scanner.next());
        checkToken(Kind.LCURLY, scanner.next());
        checkToken(Kind.RCURLY, scanner.next());
        checkToken(Kind.ASSIGN, scanner.next());
        checkToken(Kind.EQ, scanner.next());
        checkToken(Kind.EXCHANGE, scanner.next());
        checkToken(Kind.LE, scanner.next());
        checkToken(Kind.GE, scanner.next());
        checkToken(Kind.BANG, scanner.next());
        checkToken(Kind.BITAND, scanner.next());
        checkToken(Kind.AND, scanner.next());
        checkToken(Kind.BITOR, scanner.next());
        checkToken(Kind.OR, scanner.next());
        checkToken(Kind.PLUS, scanner.next());
        checkToken(Kind.MINUS, scanner.next());
        checkToken(Kind.TIMES, scanner.next());
        checkToken(Kind.EXP, scanner.next());
        checkToken(Kind.DIV, scanner.next());
        checkToken(Kind.MOD, scanner.next());
    }

    @Test
    void singleCharOps() throws LexicalException {
        String input = """
				.%?[!
				""";
        IScanner scanner = CompilerComponentFactory.makeScanner(input);
        checkToken(Kind.DOT, scanner.next());
        checkToken(Kind.MOD, scanner.next());
        checkToken(Kind.QUESTION, scanner.next());
        checkToken(Kind.LSQUARE, scanner.next());
        checkToken(Kind.BANG, scanner.next());
        checkEOF(scanner.next());
    }

    @Test
    void slackExample() throws LexicalException {
        String input = """
				"hello\\n"
				""";
        IScanner scanner = CompilerComponentFactory.makeScanner(input);
        checkString("\"hello\\n\"", "hello\n", new SourceLocation(1,1), scanner.next());
        checkEOF(scanner.next());
    }

    @Test
    void allReservedWords() throws LexicalException {
		/* reserved words: image | pixel | int | string | void | nil | load | display | write | x | y | a | r | X  | Y | Z |
          x_cart | y_cart | a_polar | r_polar | rand | sin | cos | atan  | if | while  */
        String input = """
				image pixel int string void nil load display write x y a r X Y Z x_cart y_cart a_polar r_polar rand sin cos atan if while
				""";
        IScanner scanner = CompilerComponentFactory.makeScanner(input);
        checkToken(Kind.RES_image, scanner.next());
        checkToken(Kind.RES_pixel, scanner.next());
        checkToken(Kind.RES_int, scanner.next());
        checkToken(Kind.RES_string, scanner.next());
        checkToken(Kind.RES_void, scanner.next());
        checkToken(Kind.RES_nil, scanner.next());
        checkToken(Kind.RES_load, scanner.next());
        checkToken(Kind.RES_display, scanner.next());
        checkToken(Kind.RES_write, scanner.next());
        checkToken(Kind.RES_x, scanner.next());
        checkToken(Kind.RES_y, scanner.next());
        checkToken(Kind.RES_a, scanner.next());
        checkToken(Kind.RES_r, scanner.next());
        checkToken(Kind.RES_X, scanner.next());
        checkToken(Kind.RES_Y, scanner.next());
        checkToken(Kind.RES_Z, scanner.next());
        checkToken(Kind.RES_x_cart, scanner.next());
        checkToken(Kind.RES_y_cart, scanner.next());
        checkToken(Kind.RES_a_polar, scanner.next());
        checkToken(Kind.RES_r_polar, scanner.next());
        checkToken(Kind.RES_rand, scanner.next());
        checkToken(Kind.RES_sin, scanner.next());
        checkToken(Kind.RES_cos, scanner.next());
        checkToken(Kind.RES_atan, scanner.next());
        checkToken(Kind.RES_if, scanner.next());
        checkToken(Kind.RES_while, scanner.next());
    }

    @Test
    void stringLitsLegalEsc() throws LexicalException {
        String input = """
				"\\b"
				""";
        IScanner scanner = CompilerComponentFactory.makeScanner(input);
        checkString("\"\\b\"", "\b", new SourceLocation(1,1), scanner.next());
        checkEOF(scanner.next());
    }

    @Test
    void doOperatorsSeparateTokens() throws LexicalException {
        String input = """
				doesthis+work.for-you?
				""";
        IScanner scanner = CompilerComponentFactory.makeScanner(input);
        checkToken(Kind.IDENT,scanner.next());
        checkToken(Kind.PLUS, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.DOT, scanner.next());
        // for is NOT a reserved word, oddly enough
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.MINUS, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.QUESTION, scanner.next());
    }

    @Test
    void reservedWordsWithAddedText() throws LexicalException {
        String input = """
				image imagee limage pixelx inty int stringz astring voida nill loadd load displayy ewrite write
				xx yy aa rr XX YY ZZ x_cartt y_cartt xa_polar r_polar randd sinn cosss atann iff whilee
				""";
        IScanner scanner = CompilerComponentFactory.makeScanner(input);

        checkToken(Kind.RES_image, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.RES_int, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.RES_load, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.RES_write, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.RES_r_polar, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
        checkToken(Kind.IDENT, scanner.next());
    }

    @Test
    void mathEquation2() throws LexicalException {
        String input = """
				5 * 5 = 25
				25 > 10
				7 - 3 = 4
				""";
        IScanner scanner = CompilerComponentFactory.makeScanner(input);
        checkNUM_LIT(5, scanner.next());
        checkToken(Kind.TIMES,"*", new SourceLocation(1,3), scanner.next());
        checkNUM_LIT(5, scanner.next());
        checkToken(Kind.ASSIGN,"=", new SourceLocation(1,7), scanner.next());
        checkNUM_LIT(25, scanner.next());
        checkNUM_LIT(25, scanner.next());
        checkToken(Kind.GT,">", new SourceLocation(2,4), scanner.next());
        checkNUM_LIT(10, scanner.next());
        checkNUM_LIT(7, scanner.next());
        checkToken(Kind.MINUS,"-", new SourceLocation(3,3), scanner.next());
        checkNUM_LIT(3, scanner.next());
        checkToken(Kind.ASSIGN,"=", new SourceLocation(3,7), scanner.next());
        checkNUM_LIT(4, scanner.next());
    }
}
