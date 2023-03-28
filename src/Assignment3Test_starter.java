package edu.ufl.cise.plcsp23;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import org.junit.jupiter.api.Test;

import edu.ufl.cise.plcsp23.IToken.Kind;
import edu.ufl.cise.plcsp23.ast.AST;
import edu.ufl.cise.plcsp23.ast.AssignmentStatement;
import edu.ufl.cise.plcsp23.ast.BinaryExpr;
import edu.ufl.cise.plcsp23.ast.Block;
import edu.ufl.cise.plcsp23.ast.ColorChannel;
import edu.ufl.cise.plcsp23.ast.ConditionalExpr;
import edu.ufl.cise.plcsp23.ast.Declaration;
import edu.ufl.cise.plcsp23.ast.Dimension;
import edu.ufl.cise.plcsp23.ast.Expr;
import edu.ufl.cise.plcsp23.ast.ExpandedPixelExpr;
import edu.ufl.cise.plcsp23.ast.Ident;
import edu.ufl.cise.plcsp23.ast.IdentExpr;
import edu.ufl.cise.plcsp23.ast.LValue;
import edu.ufl.cise.plcsp23.ast.NameDef;
import edu.ufl.cise.plcsp23.ast.NumLitExpr;
import edu.ufl.cise.plcsp23.ast.PixelSelector;
import edu.ufl.cise.plcsp23.ast.PixelFuncExpr;
import edu.ufl.cise.plcsp23.ast.PredeclaredVarExpr;
import edu.ufl.cise.plcsp23.ast.Program;
import edu.ufl.cise.plcsp23.ast.RandomExpr;
import edu.ufl.cise.plcsp23.ast.Statement;
import edu.ufl.cise.plcsp23.ast.StringLitExpr;
import edu.ufl.cise.plcsp23.ast.Type;
import edu.ufl.cise.plcsp23.ast.UnaryExpr;
import edu.ufl.cise.plcsp23.ast.UnaryExprPostfix;
import edu.ufl.cise.plcsp23.ast.WhileStatement;
import edu.ufl.cise.plcsp23.ast.WriteStatement;
import edu.ufl.cise.plcsp23.ast.ZExpr;

import static edu.ufl.cise.plcsp23.IToken.Kind.*;

class Assignment3Test {
	static final int TIMEOUT_MILLIS = 1000;

	/**
	 * Constructs a scanner and parser for the given input string, scans and parses the input and returns and AST.
	 *
	 * @param input String representing program to be tested
	 * @return AST representing the program
	 * @throws PLCException
	 */
	AST getAST(String input) throws PLCException {
		return CompilerComponentFactory.makeParser(input).parse();
	}

	/**
	 * Checks that the given AST e has type NumLitExpr with the indicated value.  Returns the given AST cast to NumLitExpr.
	 *
	 * @param e
	 * @param value
	 * @return
	 */
	NumLitExpr checkNumLit(AST e, int value) {
		assertThat("", e, instanceOf(NumLitExpr.class));
		NumLitExpr ne = (NumLitExpr) e;
		assertEquals(value, ne.getValue());
		return ne;
	}

	/**
	 * Checks that the given AST e has type StringLitExpr with the given String value.  Returns the given AST cast to StringLitExpr.
	 *
	 * @param e
	 * @param name
	 * @return
	 */
	StringLitExpr checkStringLit(AST e, String value) {
		assertThat("", e, instanceOf(StringLitExpr.class));
		StringLitExpr se = (StringLitExpr) e;
		assertEquals(value, se.getValue());
		return se;
	}

	/**
	 * Checks that the given AST e has type UnaryExpr with the given operator.  Returns the given AST cast to UnaryExpr.
	 *
	 * @param e
	 * @param op Kind of expected operator
	 * @return
	 */
	private UnaryExpr checkUnary(AST e, Kind op) {
		assertThat("", e, instanceOf(UnaryExpr.class));
		assertEquals(op, ((UnaryExpr) e).getOp());
		return (UnaryExpr) e;
	}


	/**
	 * Checks that the given AST e has type ConditionalExpr.  Returns the given AST cast to ConditionalExpr.
	 *
	 * @param e
	 * @return
	 */
	private ConditionalExpr checkConditional(AST e) {
		assertThat("", e, instanceOf(ConditionalExpr.class));
		return (ConditionalExpr) e;
	}

	/**
	 * Checks that the given AST e has type BinaryExpr with the given operator.  Returns the given AST cast to BinaryExpr.
	 *
	 * @param e
	 * @param op Kind of expected operator
	 * @return
	 */
	BinaryExpr checkBinary(AST e, Kind expectedOp) {
		assertThat("", e, instanceOf(BinaryExpr.class));
		BinaryExpr be = (BinaryExpr) e;
		assertEquals(expectedOp, be.getOp());
		return be;
	}

	/**
	 * Checks that the given AST e has type IdentExpr with the given name.  Returns the given AST cast to IdentExpr.
	 *
	 * @param e
	 * @param name
	 * @return
	 */
	IdentExpr checkIdentExpr(AST e, String name) {
		assertThat("", e, instanceOf(IdentExpr.class));
		IdentExpr ident = (IdentExpr) e;
		assertEquals(name, ident.getName());
		return ident;
	}

	/**
	 * Checks that the given AST e has type Ident with the given name.  Returns the given AST cast to IdentExpr.
	 *
	 * @param e
	 * @param name
	 * @return
	 */
	Ident checkIdent(AST e, String name) {
		assertThat("", e, instanceOf(Ident.class));
		Ident ident = (Ident) e;
		assertEquals(name, ident.getName());
		return ident;
	}

	NameDef checkNameDef(AST d, String name, Type type) {
		assertThat("", d, instanceOf(NameDef.class));
		NameDef def = (NameDef) d;
		assertEquals(name, def.getIdent().getName());
		assertEquals(type, def.getType());
		return def;
	}

	@Test
	void test0() throws PLCException {
		String input = """
                void d(){}
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "d");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(0, v3);
		Block v4 = v0.getBlock();
		assertThat("", v4, instanceOf(Block.class));
		List<Declaration> v5 = v4.getDecList();
		int v6 = v5.size();
		assertEquals(0, v6);
		List<Statement> v7 = v4.getStatementList();
		int v8 = v7.size();
		assertEquals(0, v8);
	}

	@Test
	void test1() throws PLCException {
		String input = """
                int d(int j) {}
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "d");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(1, v3);
		NameDef v4 = v2.get(0);
		assertThat("", v4, instanceOf(NameDef.class));
		checkNameDef(v4, "j", Type.INT);
		assertNull(v4.getDimension());
		Block v5 = v0.getBlock();
		assertThat("", v5, instanceOf(Block.class));
		List<Declaration> v6 = v5.getDecList();
		int v7 = v6.size();
		assertEquals(0, v7);
		List<Statement> v8 = v5.getStatementList();
		int v9 = v8.size();
		assertEquals(0, v9);
	}

	@Test
	void test2() throws PLCException {
		String input = """
                void prog(
                string j,
                image k){
                write x.
                		}
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "prog");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(2, v3);
		NameDef v4 = v2.get(0);
		assertThat("", v4, instanceOf(NameDef.class));
		checkNameDef(v4, "j", Type.STRING);
		assertNull(v4.getDimension());
		NameDef v5 = v2.get(1);
		assertThat("", v5, instanceOf(NameDef.class));
		checkNameDef(v5, "k", Type.IMAGE);
		assertNull(v5.getDimension());
		Block v6 = v0.getBlock();
		assertThat("", v6, instanceOf(Block.class));
		List<Declaration> v7 = v6.getDecList();
		int v8 = v7.size();
		assertEquals(0, v8);
		List<Statement> v9 = v6.getStatementList();
		int v10 = v9.size();
		assertEquals(1, v10);
		Statement v11 = v9.get(0);
		assertThat("", v11, instanceOf(WriteStatement.class));
		Expr v12 = ((WriteStatement) v11).getE();
		assertThat("", v12, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_x, ((PredeclaredVarExpr) v12).getKind());
	}

	@Test
	void test3() throws PLCException {
		String input = """
                void prog(int j, string z, image i) {}
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "prog");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(3, v3);
		NameDef v4 = v2.get(0);
		assertThat("", v4, instanceOf(NameDef.class));
		checkNameDef(v4, "j", Type.INT);
		assertNull(v4.getDimension());
		NameDef v5 = v2.get(1);
		assertThat("", v5, instanceOf(NameDef.class));
		checkNameDef(v5, "z", Type.STRING);
		assertNull(v5.getDimension());
		NameDef v6 = v2.get(2);
		assertThat("", v6, instanceOf(NameDef.class));
		checkNameDef(v6, "i", Type.IMAGE);
		assertNull(v6.getDimension());
		Block v7 = v0.getBlock();
		assertThat("", v7, instanceOf(Block.class));
		List<Declaration> v8 = v7.getDecList();
		int v9 = v8.size();
		assertEquals(0, v9);
		List<Statement> v10 = v7.getStatementList();
		int v11 = v10.size();
		assertEquals(0, v11);
	}

	@Test
	void test4() throws PLCException {
		String input = """
                int prog() {
                int s0.
                string s1.
                image s2.
                pixel s3.
                }
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "prog");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(0, v3);
		Block v4 = v0.getBlock();
		assertThat("", v4, instanceOf(Block.class));
		List<Declaration> v5 = v4.getDecList();
		int v6 = v5.size();
		assertEquals(4, v6);
		Declaration v7 = v5.get(0);
		assertThat("", v7, instanceOf(Declaration.class));
		NameDef v8 = v7.getNameDef();
		assertThat("", v8, instanceOf(NameDef.class));
		checkNameDef(v8, "s0", Type.INT);
		assertNull(v8.getDimension());
		assertNull(v7.getInitializer());
		Declaration v9 = v5.get(1);
		assertThat("", v9, instanceOf(Declaration.class));
		NameDef v10 = v9.getNameDef();
		assertThat("", v10, instanceOf(NameDef.class));
		checkNameDef(v10, "s1", Type.STRING);
		assertNull(v10.getDimension());
		assertNull(v9.getInitializer());
		Declaration v11 = v5.get(2);
		assertThat("", v11, instanceOf(Declaration.class));
		NameDef v12 = v11.getNameDef();
		assertThat("", v12, instanceOf(NameDef.class));
		checkNameDef(v12, "s2", Type.IMAGE);
		assertNull(v12.getDimension());
		assertNull(v11.getInitializer());
		Declaration v13 = v5.get(3);
		assertThat("", v13, instanceOf(Declaration.class));
		NameDef v14 = v13.getNameDef();
		assertThat("", v14, instanceOf(NameDef.class));
		checkNameDef(v14, "s3", Type.PIXEL);
		assertNull(v14.getDimension());
		assertNull(v13.getInitializer());
		List<Statement> v15 = v4.getStatementList();
		int v16 = v15.size();
		assertEquals(0, v16);
	}

	@Test
	void test5() throws PLCException {
		String input = """
                int prog() {
                int s0.
                string s1=1.
                image s2=b.
                pixel s3= sin 90.
                }
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "prog");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(0, v3);
		Block v4 = v0.getBlock();
		assertThat("", v4, instanceOf(Block.class));
		List<Declaration> v5 = v4.getDecList();
		int v6 = v5.size();
		assertEquals(4, v6);
		Declaration v7 = v5.get(0);
		assertThat("", v7, instanceOf(Declaration.class));
		NameDef v8 = v7.getNameDef();
		assertThat("", v8, instanceOf(NameDef.class));
		checkNameDef(v8, "s0", Type.INT);
		assertNull(v8.getDimension());
		assertNull(v7.getInitializer());
		Declaration v9 = v5.get(1);
		assertThat("", v9, instanceOf(Declaration.class));
		NameDef v10 = v9.getNameDef();
		assertThat("", v10, instanceOf(NameDef.class));
		checkNameDef(v10, "s1", Type.STRING);
		assertNull(v10.getDimension());
		Expr v11 = v9.getInitializer();
		checkNumLit(v11, 1);
		Declaration v12 = v5.get(2);
		assertThat("", v12, instanceOf(Declaration.class));
		NameDef v13 = v12.getNameDef();
		assertThat("", v13, instanceOf(NameDef.class));
		checkNameDef(v13, "s2", Type.IMAGE);
		assertNull(v13.getDimension());
		Expr v14 = v12.getInitializer();
		checkIdentExpr(v14, "b");
		Declaration v15 = v5.get(3);
		assertThat("", v15, instanceOf(Declaration.class));
		NameDef v16 = v15.getNameDef();
		assertThat("", v16, instanceOf(NameDef.class));
		checkNameDef(v16, "s3", Type.PIXEL);
		assertNull(v16.getDimension());
		Expr v17 = v15.getInitializer();
		checkUnary(v17, Kind.RES_sin);
		Expr v18 = ((UnaryExpr) v17).getE();
		checkNumLit(v18, 90);
		List<Statement> v19 = v4.getStatementList();
		int v20 = v19.size();
		assertEquals(0, v20);
	}

	@Test
	void test6() throws PLCException {
		String input = """
                void prog(){
                image[30,40] aa = "url".
                int xx = aa[0,0]:red.
                }
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "prog");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(0, v3);
		Block v4 = v0.getBlock();
		assertThat("", v4, instanceOf(Block.class));
		List<Declaration> v5 = v4.getDecList();
		int v6 = v5.size();
		assertEquals(2, v6);
		Declaration v7 = v5.get(0);
		assertThat("", v7, instanceOf(Declaration.class));
		NameDef v8 = v7.getNameDef();
		assertThat("", v8, instanceOf(NameDef.class));
		checkNameDef(v8, "aa", Type.IMAGE);
		Dimension v9 = ((NameDef) v8).getDimension();
		assertThat("", v9, instanceOf(Dimension.class));
		Expr v10 = ((Dimension) v9).getWidth();
		checkNumLit(v10, 30);
		Expr v11 = ((Dimension) v9).getHeight();
		checkNumLit(v11, 40);
		Expr v12 = v7.getInitializer();
		checkStringLit(v12, "url");
		Declaration v13 = v5.get(1);
		assertThat("", v13, instanceOf(Declaration.class));
		NameDef v14 = v13.getNameDef();
		assertThat("", v14, instanceOf(NameDef.class));
		checkNameDef(v14, "xx", Type.INT);
		assertNull(v14.getDimension());
		Expr v15 = v13.getInitializer();
		assertThat("", v15, instanceOf(UnaryExprPostfix.class));
		Expr v16 = ((UnaryExprPostfix) v15).getPrimary();
		checkIdentExpr(v16, "aa");
		PixelSelector v17 = ((UnaryExprPostfix) v15).getPixel();
		assertThat("", v17, instanceOf(PixelSelector.class));
		Expr v18 = ((PixelSelector) v17).getX();
		checkNumLit(v18, 0);
		Expr v19 = ((PixelSelector) v17).getY();
		checkNumLit(v19, 0);
		assertEquals(ColorChannel.red, ((UnaryExprPostfix) v15).getColor());
		List<Statement> v20 = v4.getStatementList();
		int v21 = v20.size();
		assertEquals(0, v21);
	}

	@Test
	void test7() throws PLCException {
		String input = """
                			string p() {
                			int b.
                	        image [3,40] aa.
                	        string c = "hello" + 3.
                	        aa[x,y] = bb[y,x].
                	        aa[a,r] = bb[a+90,r].

                               }

                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "p");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(0, v3);
		Block v4 = v0.getBlock();
		assertThat("", v4, instanceOf(Block.class));
		List<Declaration> v5 = v4.getDecList();
		int v6 = v5.size();
		assertEquals(3, v6);
		Declaration v7 = v5.get(0);
		assertThat("", v7, instanceOf(Declaration.class));
		NameDef v8 = v7.getNameDef();
		assertThat("", v8, instanceOf(NameDef.class));
		checkNameDef(v8, "b", Type.INT);
		assertNull(v8.getDimension());
		assertNull(v7.getInitializer());
		Declaration v9 = v5.get(1);
		assertThat("", v9, instanceOf(Declaration.class));
		NameDef v10 = v9.getNameDef();
		assertThat("", v10, instanceOf(NameDef.class));
		checkNameDef(v10, "aa", Type.IMAGE);
		Dimension v11 = ((NameDef) v10).getDimension();
		assertThat("", v11, instanceOf(Dimension.class));
		Expr v12 = ((Dimension) v11).getWidth();
		checkNumLit(v12, 3);
		Expr v13 = ((Dimension) v11).getHeight();
		checkNumLit(v13, 40);
		assertNull(v9.getInitializer());
		Declaration v14 = v5.get(2);
		assertThat("", v14, instanceOf(Declaration.class));
		NameDef v15 = v14.getNameDef();
		assertThat("", v15, instanceOf(NameDef.class));
		checkNameDef(v15, "c", Type.STRING);
		assertNull(v15.getDimension());
		Expr v16 = v14.getInitializer();

		checkBinary(v16, Kind.PLUS);
		Expr v17 = ((BinaryExpr) v16).getLeft();
		checkStringLit(v17, "hello");
		Expr v18 = ((BinaryExpr) v16).getRight();
		checkNumLit(v18, 3);
		List<Statement> v19 = v4.getStatementList();
		int v20 = v19.size();
		assertEquals(2, v20);
		Statement v21 = v19.get(0);
		assertThat("", v21, instanceOf(AssignmentStatement.class));
		LValue v22 = ((AssignmentStatement) v21).getLv();
		assertThat("", v22, instanceOf(LValue.class));
		Ident v23 = v22.getIdent();
		checkIdent(v23, "aa");
		PixelSelector v24 = v22.getPixelSelector();
		assertThat("", v24, instanceOf(PixelSelector.class));
		Expr v25 = ((PixelSelector) v24).getX();
		assertThat("", v25, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_x, ((PredeclaredVarExpr) v25).getKind());
		Expr v26 = ((PixelSelector) v24).getY();
		assertThat("", v26, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_y, ((PredeclaredVarExpr) v26).getKind());
		assertNull(((LValue) v22).getColor());
		Expr v27 = ((AssignmentStatement) v21).getE();
		assertThat("", v27, instanceOf(UnaryExprPostfix.class));
		Expr v28 = ((UnaryExprPostfix) v27).getPrimary();
		checkIdentExpr(v28, "bb");
		PixelSelector v29 = ((UnaryExprPostfix) v27).getPixel();
		assertThat("", v29, instanceOf(PixelSelector.class));
		Expr v30 = ((PixelSelector) v29).getX();
		assertThat("", v30, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_y, ((PredeclaredVarExpr) v30).getKind());
		Expr v31 = ((PixelSelector) v29).getY();
		assertThat("", v31, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_x, ((PredeclaredVarExpr) v31).getKind());
		assertNull(((UnaryExprPostfix) v27).getColor());
		Statement v32 = v19.get(1);
		assertThat("", v32, instanceOf(AssignmentStatement.class));
		LValue v33 = ((AssignmentStatement) v32).getLv();
		assertThat("", v33, instanceOf(LValue.class));
		Ident v34 = v33.getIdent();
		checkIdent(v34, "aa");
		PixelSelector v35 = v33.getPixelSelector();
		assertThat("", v35, instanceOf(PixelSelector.class));
		Expr v36 = ((PixelSelector) v35).getX();
		assertThat("", v36, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_a, ((PredeclaredVarExpr) v36).getKind());
		Expr v37 = ((PixelSelector) v35).getY();
		assertThat("", v37, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_r, ((PredeclaredVarExpr) v37).getKind());
		assertNull(((LValue) v33).getColor());
		Expr v38 = ((AssignmentStatement) v32).getE();
		assertThat("", v38, instanceOf(UnaryExprPostfix.class));
		Expr v39 = ((UnaryExprPostfix) v38).getPrimary();
		checkIdentExpr(v39, "bb");
		PixelSelector v40 = ((UnaryExprPostfix) v38).getPixel();
		assertThat("", v40, instanceOf(PixelSelector.class));
		Expr v41 = ((PixelSelector) v40).getX();

		checkBinary(v41, Kind.PLUS);
		Expr v42 = ((BinaryExpr) v41).getLeft();
		assertThat("", v42, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_a, ((PredeclaredVarExpr) v42).getKind());
		Expr v43 = ((BinaryExpr) v41).getRight();
		checkNumLit(v43, 90);
		Expr v44 = ((PixelSelector) v40).getY();
		assertThat("", v44, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_r, ((PredeclaredVarExpr) v44).getKind());
		assertNull(((UnaryExprPostfix) v38).getColor());
	}

	@Test
	void test8() throws PLCException {
		String input = """
                	image jj()
                	{
                		string[34,80] bb = "jello".
                	}
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "jj");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(0, v3);
		Block v4 = v0.getBlock();
		assertThat("", v4, instanceOf(Block.class));
		List<Declaration> v5 = v4.getDecList();
		int v6 = v5.size();
		assertEquals(1, v6);
		Declaration v7 = v5.get(0);
		assertThat("", v7, instanceOf(Declaration.class));
		NameDef v8 = v7.getNameDef();
		assertThat("", v8, instanceOf(NameDef.class));
		checkNameDef(v8, "bb", Type.STRING);
		Dimension v9 = ((NameDef) v8).getDimension();
		assertThat("", v9, instanceOf(Dimension.class));
		Expr v10 = ((Dimension) v9).getWidth();
		checkNumLit(v10, 34);
		Expr v11 = ((Dimension) v9).getHeight();
		checkNumLit(v11, 80);
		Expr v12 = v7.getInitializer();
		checkStringLit(v12, "jello");
		List<Statement> v13 = v4.getStatementList();
		int v14 = v13.size();
		assertEquals(0, v14);
	}

	@Test
	void test9() throws PLCException {
		String input = """
                void p(){
                   int z.
                   while z {
                       while m {
                          string z.
                       }.
                    }.
                    }
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "p");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(0, v3);
		Block v4 = v0.getBlock();
		assertThat("", v4, instanceOf(Block.class));
		List<Declaration> v5 = v4.getDecList();
		int v6 = v5.size();
		assertEquals(1, v6);
		Declaration v7 = v5.get(0);
		assertThat("", v7, instanceOf(Declaration.class));
		NameDef v8 = v7.getNameDef();
		assertThat("", v8, instanceOf(NameDef.class));
		checkNameDef(v8, "z", Type.INT);
		assertNull(v8.getDimension());
		assertNull(v7.getInitializer());
		List<Statement> v9 = v4.getStatementList();
		int v10 = v9.size();
		assertEquals(1, v10);
		Statement v11 = v9.get(0);
		assertThat("", v11, instanceOf(WhileStatement.class));
		Expr v12 = ((WhileStatement) v11).getGuard();
		checkIdentExpr(v12, "z");
		Block v13 = ((WhileStatement) v11).getBlock();
		assertThat("", v13, instanceOf(Block.class));
		List<Declaration> v14 = v13.getDecList();
		int v15 = v14.size();
		assertEquals(0, v15);
		List<Statement> v16 = v13.getStatementList();
		int v17 = v16.size();
		assertEquals(1, v17);
		Statement v18 = v16.get(0);
		assertThat("", v18, instanceOf(WhileStatement.class));
		Expr v19 = ((WhileStatement) v18).getGuard();
		checkIdentExpr(v19, "m");
		Block v20 = ((WhileStatement) v18).getBlock();
		assertThat("", v20, instanceOf(Block.class));
		List<Declaration> v21 = v20.getDecList();
		int v22 = v21.size();
		assertEquals(1, v22);
		Declaration v23 = v21.get(0);
		assertThat("", v23, instanceOf(Declaration.class));
		NameDef v24 = v23.getNameDef();
		assertThat("", v24, instanceOf(NameDef.class));
		checkNameDef(v24, "z", Type.STRING);
		assertNull(v24.getDimension());
		assertNull(v23.getInitializer());
		List<Statement> v25 = v20.getStatementList();
		int v26 = v25.size();
		assertEquals(0, v26);
	}

	@Test
	void test10() throws PLCException {
		String input = """
                image gen(){
                pixel p = [33,44,56].
                }
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "gen");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(0, v3);
		Block v4 = v0.getBlock();
		assertThat("", v4, instanceOf(Block.class));
		List<Declaration> v5 = v4.getDecList();
		int v6 = v5.size();
		assertEquals(1, v6);
		Declaration v7 = v5.get(0);
		assertThat("", v7, instanceOf(Declaration.class));
		NameDef v8 = v7.getNameDef();
		assertThat("", v8, instanceOf(NameDef.class));
		checkNameDef(v8, "p", Type.PIXEL);
		assertNull(v8.getDimension());
		Expr v9 = v7.getInitializer();
		assertThat("", v9, instanceOf(ExpandedPixelExpr.class));
		Expr v10 = ((ExpandedPixelExpr) v9).getRedExpr();
		checkNumLit(v10, 33);
		Expr v11 = ((ExpandedPixelExpr) v9).getGrnExpr();
		checkNumLit(v11, 44);
		Expr v12 = ((ExpandedPixelExpr) v9).getBluExpr();
		checkNumLit(v12, 56);
		List<Statement> v13 = v4.getStatementList();
		int v14 = v13.size();
		assertEquals(0, v14);
	}

	@Test
	void test11() throws PLCException {
		String input = """
                string s(){
                image j.
                j[x,y] = k[x_cart [a,r], y_cart [a,r]].
                j[a,r] = k[a_polar [x,y], r_polar [y,x]].
                }
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "s");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(0, v3);
		Block v4 = v0.getBlock();
		assertThat("", v4, instanceOf(Block.class));
		List<Declaration> v5 = v4.getDecList();
		int v6 = v5.size();
		assertEquals(1, v6);
		Declaration v7 = v5.get(0);
		assertThat("", v7, instanceOf(Declaration.class));
		NameDef v8 = v7.getNameDef();
		assertThat("", v8, instanceOf(NameDef.class));
		checkNameDef(v8, "j", Type.IMAGE);
		assertNull(v8.getDimension());
		assertNull(v7.getInitializer());
		List<Statement> v9 = v4.getStatementList();
		int v10 = v9.size();
		assertEquals(2, v10);
		Statement v11 = v9.get(0);
		assertThat("", v11, instanceOf(AssignmentStatement.class));
		LValue v12 = ((AssignmentStatement) v11).getLv();
		assertThat("", v12, instanceOf(LValue.class));
		Ident v13 = v12.getIdent();
		checkIdent(v13, "j");
		PixelSelector v14 = v12.getPixelSelector();
		assertThat("", v14, instanceOf(PixelSelector.class));
		Expr v15 = ((PixelSelector) v14).getX();
		assertThat("", v15, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_x, ((PredeclaredVarExpr) v15).getKind());
		Expr v16 = ((PixelSelector) v14).getY();
		assertThat("", v16, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_y, ((PredeclaredVarExpr) v16).getKind());
		assertNull(((LValue) v12).getColor());
		Expr v17 = ((AssignmentStatement) v11).getE();
		assertThat("", v17, instanceOf(UnaryExprPostfix.class));
		Expr v18 = ((UnaryExprPostfix) v17).getPrimary();
		checkIdentExpr(v18, "k");
		PixelSelector v19 = ((UnaryExprPostfix) v17).getPixel();
		assertThat("", v19, instanceOf(PixelSelector.class));
		Expr v20 = ((PixelSelector) v19).getX();
		assertThat("", v20, instanceOf(PixelFuncExpr.class));
		assertEquals(RES_x_cart, ((PixelFuncExpr) v20).getFunction());
		PixelSelector v21 = ((PixelFuncExpr) v20).getSelector();
		assertThat("", v21, instanceOf(PixelSelector.class));
		Expr v22 = ((PixelSelector) v21).getX();
		assertThat("", v22, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_a, ((PredeclaredVarExpr) v22).getKind());
		Expr v23 = ((PixelSelector) v21).getY();
		assertThat("", v23, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_r, ((PredeclaredVarExpr) v23).getKind());
		Expr v24 = ((PixelSelector) v19).getY();
		assertThat("", v24, instanceOf(PixelFuncExpr.class));
		assertEquals(RES_y_cart, ((PixelFuncExpr) v24).getFunction());
		PixelSelector v25 = ((PixelFuncExpr) v24).getSelector();
		assertThat("", v25, instanceOf(PixelSelector.class));
		Expr v26 = ((PixelSelector) v25).getX();
		assertThat("", v26, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_a, ((PredeclaredVarExpr) v26).getKind());
		Expr v27 = ((PixelSelector) v25).getY();
		assertThat("", v27, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_r, ((PredeclaredVarExpr) v27).getKind());
		assertNull(((UnaryExprPostfix) v17).getColor());
		Statement v28 = v9.get(1);
		assertThat("", v28, instanceOf(AssignmentStatement.class));
		LValue v29 = ((AssignmentStatement) v28).getLv();
		assertThat("", v29, instanceOf(LValue.class));
		Ident v30 = v29.getIdent();
		checkIdent(v30, "j");
		PixelSelector v31 = v29.getPixelSelector();
		assertThat("", v31, instanceOf(PixelSelector.class));
		Expr v32 = ((PixelSelector) v31).getX();
		assertThat("", v32, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_a, ((PredeclaredVarExpr) v32).getKind());
		Expr v33 = ((PixelSelector) v31).getY();
		assertThat("", v33, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_r, ((PredeclaredVarExpr) v33).getKind());
		assertNull(((LValue) v29).getColor());
		Expr v34 = ((AssignmentStatement) v28).getE();
		assertThat("", v34, instanceOf(UnaryExprPostfix.class));
		Expr v35 = ((UnaryExprPostfix) v34).getPrimary();
		checkIdentExpr(v35, "k");
		PixelSelector v36 = ((UnaryExprPostfix) v34).getPixel();
		assertThat("", v36, instanceOf(PixelSelector.class));
		Expr v37 = ((PixelSelector) v36).getX();
		assertThat("", v37, instanceOf(PixelFuncExpr.class));
		assertEquals(RES_a_polar, ((PixelFuncExpr) v37).getFunction());
		PixelSelector v38 = ((PixelFuncExpr) v37).getSelector();
		assertThat("", v38, instanceOf(PixelSelector.class));
		Expr v39 = ((PixelSelector) v38).getX();
		assertThat("", v39, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_x, ((PredeclaredVarExpr) v39).getKind());
		Expr v40 = ((PixelSelector) v38).getY();
		assertThat("", v40, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_y, ((PredeclaredVarExpr) v40).getKind());
		Expr v41 = ((PixelSelector) v36).getY();
		assertThat("", v41, instanceOf(PixelFuncExpr.class));
		assertEquals(RES_r_polar, ((PixelFuncExpr) v41).getFunction());
		PixelSelector v42 = ((PixelFuncExpr) v41).getSelector();
		assertThat("", v42, instanceOf(PixelSelector.class));
		Expr v43 = ((PixelSelector) v42).getX();
		assertThat("", v43, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_y, ((PredeclaredVarExpr) v43).getKind());
		Expr v44 = ((PixelSelector) v42).getY();
		assertThat("", v44, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_x, ((PredeclaredVarExpr) v44).getKind());
		assertNull(((UnaryExprPostfix) v34).getColor());
	}

	@Test
	void test12() throws PLCException {
		String input = """
                string s(){
                int xx = rand.
                int yy = Z.
                }
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "s");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(0, v3);
		Block v4 = v0.getBlock();
		assertThat("", v4, instanceOf(Block.class));
		List<Declaration> v5 = v4.getDecList();
		int v6 = v5.size();
		assertEquals(2, v6);
		Declaration v7 = v5.get(0);
		assertThat("", v7, instanceOf(Declaration.class));
		NameDef v8 = v7.getNameDef();
		assertThat("", v8, instanceOf(NameDef.class));
		checkNameDef(v8, "xx", Type.INT);
		assertNull(v8.getDimension());
		Expr v9 = v7.getInitializer();
		assertThat("", v9, instanceOf(RandomExpr.class));
		Declaration v10 = v5.get(1);
		assertThat("", v10, instanceOf(Declaration.class));
		NameDef v11 = v10.getNameDef();
		assertThat("", v11, instanceOf(NameDef.class));
		checkNameDef(v11, "yy", Type.INT);
		assertNull(v11.getDimension());
		Expr v12 = v10.getInitializer();
		assertThat("", v12, instanceOf(ZExpr.class));
		List<Statement> v13 = v4.getStatementList();
		int v14 = v13.size();
		assertEquals(0, v14);
	}

	@Test
	void test13() throws PLCException {
		String input = """
                image aa(){}
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "aa");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(0, v3);
		Block v4 = v0.getBlock();
		assertThat("", v4, instanceOf(Block.class));
		List<Declaration> v5 = v4.getDecList();
		int v6 = v5.size();
		assertEquals(0, v6);
		List<Statement> v7 = v4.getStatementList();
		int v8 = v7.size();
		assertEquals(0, v8);
	}

	@Test
	void test14() throws PLCException {
		String input = """
                pixel aa(){}
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "aa");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(0, v3);
		Block v4 = v0.getBlock();
		assertThat("", v4, instanceOf(Block.class));
		List<Declaration> v5 = v4.getDecList();
		int v6 = v5.size();
		assertEquals(0, v6);
		List<Statement> v7 = v4.getStatementList();
		int v8 = v7.size();
		assertEquals(0, v8);
	}

	@Test
	void test15() throws PLCException {
		String input = """
                string aa(){}
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "aa");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(0, v3);
		Block v4 = v0.getBlock();
		assertThat("", v4, instanceOf(Block.class));
		List<Declaration> v5 = v4.getDecList();
		int v6 = v5.size();
		assertEquals(0, v6);
		List<Statement> v7 = v4.getStatementList();
		int v8 = v7.size();
		assertEquals(0, v8);
	}

	@Test
	void test16() throws PLCException {
		String input = """
                int aa(){}
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "aa");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(0, v3);
		Block v4 = v0.getBlock();
		assertThat("", v4, instanceOf(Block.class));
		List<Declaration> v5 = v4.getDecList();
		int v6 = v5.size();
		assertEquals(0, v6);
		List<Statement> v7 = v4.getStatementList();
		int v8 = v7.size();
		assertEquals(0, v8);
	}

	@Test
	void test17() throws PLCException {
		String input = """
                void aa(int j){}
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "aa");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(1, v3);
		NameDef v4 = v2.get(0);
		assertThat("", v4, instanceOf(NameDef.class));
		checkNameDef(v4, "j", Type.INT);
		assertNull(v4.getDimension());
		Block v5 = v0.getBlock();
		assertThat("", v5, instanceOf(Block.class));
		List<Declaration> v6 = v5.getDecList();
		int v7 = v6.size();
		assertEquals(0, v7);
		List<Statement> v8 = v5.getStatementList();
		int v9 = v8.size();
		assertEquals(0, v9);
	}

	@Test
	void test18() throws PLCException {
		String input = """
                void aa(int q, pixel w, string e){}
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "aa");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(3, v3);
		NameDef v4 = v2.get(0);
		assertThat("", v4, instanceOf(NameDef.class));
		checkNameDef(v4, "q", Type.INT);
		assertNull(v4.getDimension());
		NameDef v5 = v2.get(1);
		assertThat("", v5, instanceOf(NameDef.class));
		checkNameDef(v5, "w", Type.PIXEL);
		assertNull(v5.getDimension());
		NameDef v6 = v2.get(2);
		assertThat("", v6, instanceOf(NameDef.class));
		checkNameDef(v6, "e", Type.STRING);
		assertNull(v6.getDimension());
		Block v7 = v0.getBlock();
		assertThat("", v7, instanceOf(Block.class));
		List<Declaration> v8 = v7.getDecList();
		int v9 = v8.size();
		assertEquals(0, v9);
		List<Statement> v10 = v7.getStatementList();
		int v11 = v10.size();
		assertEquals(0, v11);
	}

	@Test
	void test19() throws PLCException {
		String input = """
                void aa(int q, pixel w, string[5+3-(!4), "abc"] e){}
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "aa");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(3, v3);
		NameDef v4 = v2.get(0);
		assertThat("", v4, instanceOf(NameDef.class));
		checkNameDef(v4, "q", Type.INT);
		assertNull(v4.getDimension());
		NameDef v5 = v2.get(1);
		assertThat("", v5, instanceOf(NameDef.class));
		checkNameDef(v5, "w", Type.PIXEL);
		assertNull(v5.getDimension());
		NameDef v6 = v2.get(2);
		assertThat("", v6, instanceOf(NameDef.class));
		checkNameDef(v6, "e", Type.STRING);
		Dimension v7 = ((NameDef) v6).getDimension();
		assertThat("", v7, instanceOf(Dimension.class));
		Expr v8 = ((Dimension) v7).getWidth();

		checkBinary(v8, Kind.MINUS);
		Expr v9 = ((BinaryExpr) v8).getLeft();

		checkBinary(v9, Kind.PLUS);
		Expr v10 = ((BinaryExpr) v9).getLeft();
		checkNumLit(v10, 5);
		Expr v11 = ((BinaryExpr) v9).getRight();
		checkNumLit(v11, 3);
		Expr v12 = ((BinaryExpr) v8).getRight();
		checkUnary(v12, Kind.BANG);
		Expr v13 = ((UnaryExpr) v12).getE();
		checkNumLit(v13, 4);
		Expr v14 = ((Dimension) v7).getHeight();
		checkStringLit(v14, "abc");
		Block v15 = v0.getBlock();
		assertThat("", v15, instanceOf(Block.class));
		List<Declaration> v16 = v15.getDecList();
		int v17 = v16.size();
		assertEquals(0, v17);
		List<Statement> v18 = v15.getStatementList();
		int v19 = v18.size();
		assertEquals(0, v19);
	}

	@Test
	void test20() throws PLCException {
		String input = """
                image aa(string[5+3-(!4), "abc"] e){
                int[5, 10] q.
                string abc = "abc"+12.
                while (32 / "str") == ident + Z & rand - cos (52 <= 12 ** ! ("qwe" >= ((_abc) | 321) < "123" > abc ** 462) - 156 && 12 % (1 && atan (0)) - 0 * 12 || (8 --sin 9)){
                int abc.
                string abc = 5+13.
                }.
                }
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "aa");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(1, v3);
		NameDef v4 = v2.get(0);
		assertThat("", v4, instanceOf(NameDef.class));
		checkNameDef(v4, "e", Type.STRING);
		Dimension v5 = ((NameDef) v4).getDimension();
		assertThat("", v5, instanceOf(Dimension.class));
		Expr v6 = ((Dimension) v5).getWidth();

		checkBinary(v6, Kind.MINUS);
		Expr v7 = ((BinaryExpr) v6).getLeft();

		checkBinary(v7, Kind.PLUS);
		Expr v8 = ((BinaryExpr) v7).getLeft();
		checkNumLit(v8, 5);
		Expr v9 = ((BinaryExpr) v7).getRight();
		checkNumLit(v9, 3);
		Expr v10 = ((BinaryExpr) v6).getRight();
		checkUnary(v10, Kind.BANG);
		Expr v11 = ((UnaryExpr) v10).getE();
		checkNumLit(v11, 4);
		Expr v12 = ((Dimension) v5).getHeight();
		checkStringLit(v12, "abc");
		Block v13 = v0.getBlock();
		assertThat("", v13, instanceOf(Block.class));
		List<Declaration> v14 = v13.getDecList();
		int v15 = v14.size();
		assertEquals(2, v15);
		Declaration v16 = v14.get(0);
		assertThat("", v16, instanceOf(Declaration.class));
		NameDef v17 = v16.getNameDef();
		assertThat("", v17, instanceOf(NameDef.class));
		checkNameDef(v17, "q", Type.INT);
		Dimension v18 = ((NameDef) v17).getDimension();
		assertThat("", v18, instanceOf(Dimension.class));
		Expr v19 = ((Dimension) v18).getWidth();
		checkNumLit(v19, 5);
		Expr v20 = ((Dimension) v18).getHeight();
		checkNumLit(v20, 10);
		assertNull(v16.getInitializer());
		Declaration v21 = v14.get(1);
		assertThat("", v21, instanceOf(Declaration.class));
		NameDef v22 = v21.getNameDef();
		assertThat("", v22, instanceOf(NameDef.class));
		checkNameDef(v22, "abc", Type.STRING);
		assertNull(v22.getDimension());
		Expr v23 = v21.getInitializer();

		checkBinary(v23, Kind.PLUS);
		Expr v24 = ((BinaryExpr) v23).getLeft();
		checkStringLit(v24, "abc");
		Expr v25 = ((BinaryExpr) v23).getRight();
		checkNumLit(v25, 12);
		List<Statement> v26 = v13.getStatementList();
		int v27 = v26.size();
		assertEquals(1, v27);
		Statement v28 = v26.get(0);
		assertThat("", v28, instanceOf(WhileStatement.class));
		Expr v29 = ((WhileStatement) v28).getGuard();

		checkBinary(v29, Kind.BITAND);
		Expr v30 = ((BinaryExpr) v29).getLeft();

		checkBinary(v30, Kind.EQ);
		Expr v31 = ((BinaryExpr) v30).getLeft();

		checkBinary(v31, Kind.DIV);
		Expr v32 = ((BinaryExpr) v31).getLeft();
		checkNumLit(v32, 32);
		Expr v33 = ((BinaryExpr) v31).getRight();
		checkStringLit(v33, "str");
		Expr v34 = ((BinaryExpr) v30).getRight();

		checkBinary(v34, Kind.PLUS);
		Expr v35 = ((BinaryExpr) v34).getLeft();
		checkIdentExpr(v35, "ident");
		Expr v36 = ((BinaryExpr) v34).getRight();
		assertThat("", v36, instanceOf(ZExpr.class));
		Expr v37 = ((BinaryExpr) v29).getRight();

		checkBinary(v37, Kind.MINUS);
		Expr v38 = ((BinaryExpr) v37).getLeft();
		assertThat("", v38, instanceOf(RandomExpr.class));
		Expr v39 = ((BinaryExpr) v37).getRight();
		checkUnary(v39, Kind.RES_cos);
		Expr v40 = ((UnaryExpr) v39).getE();

		checkBinary(v40, Kind.OR);
		Expr v41 = ((BinaryExpr) v40).getLeft();

		checkBinary(v41, Kind.AND);
		Expr v42 = ((BinaryExpr) v41).getLeft();

		checkBinary(v42, Kind.LE);
		Expr v43 = ((BinaryExpr) v42).getLeft();
		checkNumLit(v43, 52);
		Expr v44 = ((BinaryExpr) v42).getRight();

		checkBinary(v44, Kind.EXP);
		Expr v45 = ((BinaryExpr) v44).getLeft();
		checkNumLit(v45, 12);
		Expr v46 = ((BinaryExpr) v44).getRight();

		checkBinary(v46, Kind.MINUS);
		Expr v47 = ((BinaryExpr) v46).getLeft();
		checkUnary(v47, Kind.BANG);
		Expr v48 = ((UnaryExpr) v47).getE();

		checkBinary(v48, Kind.GT);
		Expr v49 = ((BinaryExpr) v48).getLeft();

		checkBinary(v49, Kind.LT);
		Expr v50 = ((BinaryExpr) v49).getLeft();

		checkBinary(v50, Kind.GE);
		Expr v51 = ((BinaryExpr) v50).getLeft();
		checkStringLit(v51, "qwe");
		Expr v52 = ((BinaryExpr) v50).getRight();

		checkBinary(v52, Kind.BITOR);
		Expr v53 = ((BinaryExpr) v52).getLeft();
		checkIdentExpr(v53, "_abc");
		Expr v54 = ((BinaryExpr) v52).getRight();
		checkNumLit(v54, 321);
		Expr v55 = ((BinaryExpr) v49).getRight();
		checkStringLit(v55, "123");
		Expr v56 = ((BinaryExpr) v48).getRight();

		checkBinary(v56, Kind.EXP);
		Expr v57 = ((BinaryExpr) v56).getLeft();
		checkIdentExpr(v57, "abc");
		Expr v58 = ((BinaryExpr) v56).getRight();
		checkNumLit(v58, 462);
		Expr v59 = ((BinaryExpr) v46).getRight();
		checkNumLit(v59, 156);
		Expr v60 = ((BinaryExpr) v41).getRight();

		checkBinary(v60, Kind.MINUS);
		Expr v61 = ((BinaryExpr) v60).getLeft();

		checkBinary(v61, Kind.MOD);
		Expr v62 = ((BinaryExpr) v61).getLeft();
		checkNumLit(v62, 12);
		Expr v63 = ((BinaryExpr) v61).getRight();

		checkBinary(v63, Kind.AND);
		Expr v64 = ((BinaryExpr) v63).getLeft();
		checkNumLit(v64, 1);
		Expr v65 = ((BinaryExpr) v63).getRight();
		checkUnary(v65, Kind.RES_atan);
		Expr v66 = ((UnaryExpr) v65).getE();
		checkNumLit(v66, 0);
		Expr v67 = ((BinaryExpr) v60).getRight();

		checkBinary(v67, Kind.TIMES);
		Expr v68 = ((BinaryExpr) v67).getLeft();
		checkNumLit(v68, 0);
		Expr v69 = ((BinaryExpr) v67).getRight();
		checkNumLit(v69, 12);
		Expr v70 = ((BinaryExpr) v40).getRight();

		checkBinary(v70, Kind.MINUS);
		Expr v71 = ((BinaryExpr) v70).getLeft();
		checkNumLit(v71, 8);
		Expr v72 = ((BinaryExpr) v70).getRight();
		checkUnary(v72, Kind.MINUS);
		Expr v73 = ((UnaryExpr) v72).getE();
		checkUnary(v73, Kind.RES_sin);
		Expr v74 = ((UnaryExpr) v73).getE();
		checkNumLit(v74, 9);
		Block v75 = ((WhileStatement) v28).getBlock();
		assertThat("", v75, instanceOf(Block.class));
		List<Declaration> v76 = v75.getDecList();
		int v77 = v76.size();
		assertEquals(2, v77);
		Declaration v78 = v76.get(0);
		assertThat("", v78, instanceOf(Declaration.class));
		NameDef v79 = v78.getNameDef();
		assertThat("", v79, instanceOf(NameDef.class));
		checkNameDef(v79, "abc", Type.INT);
		assertNull(v79.getDimension());
		assertNull(v78.getInitializer());
		Declaration v80 = v76.get(1);
		assertThat("", v80, instanceOf(Declaration.class));
		NameDef v81 = v80.getNameDef();
		assertThat("", v81, instanceOf(NameDef.class));
		checkNameDef(v81, "abc", Type.STRING);
		assertNull(v81.getDimension());
		Expr v82 = v80.getInitializer();

		checkBinary(v82, Kind.PLUS);
		Expr v83 = ((BinaryExpr) v82).getLeft();
		checkNumLit(v83, 5);
		Expr v84 = ((BinaryExpr) v82).getRight();
		checkNumLit(v84, 13);
		List<Statement> v85 = v75.getStatementList();
		int v86 = v85.size();
		assertEquals(0, v86);
	}

	@Test
	void test21() throws PLCException {
		String input = """
                void aa(int j){
                write (32[a,d+e]:red / "str") == [12,45,abc]:red + Z & rand - cos (52 <= 12 ** ! ("qwe" >= ((_abc) | 321) < "123" > abc ** 462) - 156 && 12 % (1 && atan (0)) - 0 * 12 || (8 --sin 9:red)).
                }
                """;
		AST ast = getAST(input);
		assertThat("", ast, instanceOf(Program.class));
		Program v0 = (Program) ast;
		Ident v1 = v0.getIdent();
		checkIdent(v1, "aa");
		List<NameDef> v2 = v0.getParamList();
		int v3 = v2.size();
		assertEquals(1, v3);
		NameDef v4 = v2.get(0);
		assertThat("", v4, instanceOf(NameDef.class));
		checkNameDef(v4, "j", Type.INT);
		assertNull(v4.getDimension());
		Block v5 = v0.getBlock();
		assertThat("", v5, instanceOf(Block.class));
		List<Declaration> v6 = v5.getDecList();
		int v7 = v6.size();
		assertEquals(0, v7);
		List<Statement> v8 = v5.getStatementList();
		int v9 = v8.size();
		assertEquals(1, v9);
		Statement v10 = v8.get(0);
		assertThat("", v10, instanceOf(WriteStatement.class));
		Expr v11 = ((WriteStatement) v10).getE();

		checkBinary(v11, Kind.BITAND);
		Expr v12 = ((BinaryExpr) v11).getLeft();

		checkBinary(v12, Kind.EQ);
		Expr v13 = ((BinaryExpr) v12).getLeft();

		checkBinary(v13, Kind.DIV);
		Expr v14 = ((BinaryExpr) v13).getLeft();
		assertThat("", v14, instanceOf(UnaryExprPostfix.class));
		Expr v15 = ((UnaryExprPostfix) v14).getPrimary();
		checkNumLit(v15, 32);
		PixelSelector v16 = ((UnaryExprPostfix) v14).getPixel();
		assertThat("", v16, instanceOf(PixelSelector.class));
		Expr v17 = ((PixelSelector) v16).getX();
		assertThat("", v17, instanceOf(PredeclaredVarExpr.class));
		assertEquals(RES_a, ((PredeclaredVarExpr) v17).getKind());
		Expr v18 = ((PixelSelector) v16).getY();

		checkBinary(v18, Kind.PLUS);
		Expr v19 = ((BinaryExpr) v18).getLeft();
		checkIdentExpr(v19, "d");
		Expr v20 = ((BinaryExpr) v18).getRight();
		checkIdentExpr(v20, "e");
		assertEquals(ColorChannel.red, ((UnaryExprPostfix) v14).getColor());
		Expr v21 = ((BinaryExpr) v13).getRight();
		checkStringLit(v21, "str");
		Expr v22 = ((BinaryExpr) v12).getRight();

		checkBinary(v22, Kind.PLUS);
		Expr v23 = ((BinaryExpr) v22).getLeft();
		assertThat("", v23, instanceOf(UnaryExprPostfix.class));
		Expr v24 = ((UnaryExprPostfix) v23).getPrimary();
		assertThat("", v24, instanceOf(ExpandedPixelExpr.class));
		Expr v25 = ((ExpandedPixelExpr) v24).getRedExpr();
		checkNumLit(v25, 12);
		Expr v26 = ((ExpandedPixelExpr) v24).getGrnExpr();
		checkNumLit(v26, 45);
		Expr v27 = ((ExpandedPixelExpr) v24).getBluExpr();
		checkIdentExpr(v27, "abc");
		assertNull(((UnaryExprPostfix) v23).getPixel());
		assertEquals(ColorChannel.red, ((UnaryExprPostfix) v23).getColor());
		Expr v28 = ((BinaryExpr) v22).getRight();
		assertThat("", v28, instanceOf(ZExpr.class));
		Expr v29 = ((BinaryExpr) v11).getRight();

		checkBinary(v29, Kind.MINUS);
		Expr v30 = ((BinaryExpr) v29).getLeft();
		assertThat("", v30, instanceOf(RandomExpr.class));
		Expr v31 = ((BinaryExpr) v29).getRight();
		checkUnary(v31, Kind.RES_cos);
		Expr v32 = ((UnaryExpr) v31).getE();

		checkBinary(v32, Kind.OR);
		Expr v33 = ((BinaryExpr) v32).getLeft();

		checkBinary(v33, Kind.AND);
		Expr v34 = ((BinaryExpr) v33).getLeft();

		checkBinary(v34, Kind.LE);
		Expr v35 = ((BinaryExpr) v34).getLeft();
		checkNumLit(v35, 52);
		Expr v36 = ((BinaryExpr) v34).getRight();

		checkBinary(v36, Kind.EXP);
		Expr v37 = ((BinaryExpr) v36).getLeft();
		checkNumLit(v37, 12);
		Expr v38 = ((BinaryExpr) v36).getRight();

		checkBinary(v38, Kind.MINUS);
		Expr v39 = ((BinaryExpr) v38).getLeft();
		checkUnary(v39, Kind.BANG);
		Expr v40 = ((UnaryExpr) v39).getE();

		checkBinary(v40, Kind.GT);
		Expr v41 = ((BinaryExpr) v40).getLeft();

		checkBinary(v41, Kind.LT);
		Expr v42 = ((BinaryExpr) v41).getLeft();

		checkBinary(v42, Kind.GE);
		Expr v43 = ((BinaryExpr) v42).getLeft();
		checkStringLit(v43, "qwe");
		Expr v44 = ((BinaryExpr) v42).getRight();

		checkBinary(v44, Kind.BITOR);
		Expr v45 = ((BinaryExpr) v44).getLeft();
		checkIdentExpr(v45, "_abc");
		Expr v46 = ((BinaryExpr) v44).getRight();
		checkNumLit(v46, 321);
		Expr v47 = ((BinaryExpr) v41).getRight();
		checkStringLit(v47, "123");
		Expr v48 = ((BinaryExpr) v40).getRight();

		checkBinary(v48, Kind.EXP);
		Expr v49 = ((BinaryExpr) v48).getLeft();
		checkIdentExpr(v49, "abc");
		Expr v50 = ((BinaryExpr) v48).getRight();
		checkNumLit(v50, 462);
		Expr v51 = ((BinaryExpr) v38).getRight();
		checkNumLit(v51, 156);
		Expr v52 = ((BinaryExpr) v33).getRight();

		checkBinary(v52, Kind.MINUS);
		Expr v53 = ((BinaryExpr) v52).getLeft();

		checkBinary(v53, Kind.MOD);
		Expr v54 = ((BinaryExpr) v53).getLeft();
		checkNumLit(v54, 12);
		Expr v55 = ((BinaryExpr) v53).getRight();

		checkBinary(v55, Kind.AND);
		Expr v56 = ((BinaryExpr) v55).getLeft();
		checkNumLit(v56, 1);
		Expr v57 = ((BinaryExpr) v55).getRight();
		checkUnary(v57, Kind.RES_atan);
		Expr v58 = ((UnaryExpr) v57).getE();
		checkNumLit(v58, 0);
		Expr v59 = ((BinaryExpr) v52).getRight();

		checkBinary(v59, Kind.TIMES);
		Expr v60 = ((BinaryExpr) v59).getLeft();
		checkNumLit(v60, 0);
		Expr v61 = ((BinaryExpr) v59).getRight();
		checkNumLit(v61, 12);
		Expr v62 = ((BinaryExpr) v32).getRight();

		checkBinary(v62, Kind.MINUS);
		Expr v63 = ((BinaryExpr) v62).getLeft();
		checkNumLit(v63, 8);
		Expr v64 = ((BinaryExpr) v62).getRight();
		checkUnary(v64, Kind.MINUS);
		Expr v65 = ((UnaryExpr) v64).getE();
		checkUnary(v65, Kind.RES_sin);
		Expr v66 = ((UnaryExpr) v65).getE();
		assertThat("", v66, instanceOf(UnaryExprPostfix.class));
		Expr v67 = ((UnaryExprPostfix) v66).getPrimary();
		checkNumLit(v67, 9);
		assertNull(((UnaryExprPostfix) v66).getPixel());
		assertEquals(ColorChannel.red, ((UnaryExprPostfix) v66).getColor());
	}

	@Test
	void test22() throws PLCException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
                    prog s(){
                    xx = 22
                    }
                    """;
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

	@Test
	void test23() throws PLCException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
                    void s(){
                    xx = 22;
                    }
                    """;
			assertThrows(LexicalException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

	@Test
	void test24() throws PLCException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
                    void a(){}
                    """;
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

	@Test
	void test25() throws PLCException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
                    int s q q{}
                    """;
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

	@Test
	void test26() throws PLCException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
                    int s()..
                    """;
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

	@Test
	void test27() throws PLCException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
                    int s(,int i){}
                    """;
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

	@Test
	void test28() throws PLCException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
                    int s()
                    """;
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

	@Test
	void test29() throws PLCException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
                    int s(){
                    """;
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

	@Test
	void test30() throws PLCException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
                    image img(){} extra
                    """;
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

	@Test
	void test31() throws PLCException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
                    image img(abc abc){}
                    """;
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

	@Test
	void test32() throws PLCException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
                    image img(int a){}
                    """;
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

	@Test
	void test33() throws PLCException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
                    image img(int aa=5){}
                    """;
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

	@Test
	void test34() throws PLCException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
                    image img(int aa. string bb){}
                    """;
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

	@Test
	void test35() throws PLCException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
                    void prog(int aa){
                    int b = 5.
                    write b.
                    int c = 6.
                    }
                    """;
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

	@Test
	void test36() throws PLCException {
		assertTimeoutPreemptively(Duration.ofMillis(TIMEOUT_MILLIS), () -> {
			String input = """
                    void vv(int abc){
                    while q {
                    int i.
                    ..
                    }
                    """;
			assertThrows(SyntaxException.class, () -> {
				@SuppressWarnings("unused")
				AST ast = getAST(input);
			});
		});
	}

}