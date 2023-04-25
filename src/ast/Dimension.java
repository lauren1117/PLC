/*Copyright 2023 by Beverly A Sanders
 * 
 * This code is provided for solely for use of students in COP4020 Programming Language Concepts at the 
 * University of Florida during the spring semester 2023 as part of the course project.  
 * 
 * No other use is authorized. 
 * 
 * This code may not be posted on a public web site either during or after the course.  
 */

package edu.ufl.cise.plcsp23.ast;

import edu.ufl.cise.plcsp23.IToken;
import edu.ufl.cise.plcsp23.PLCException;

public class Dimension extends AST {

	final Expr width;
	final Expr height;
	Integer w;
	Integer h;

	public Dimension(IToken firstToken, Expr width, Expr height) {
		super(firstToken);
		this.width = width;
		this.height = height;
		this.w = 0;
		this.h = 0;
	}

	@Override
	public Object visit(ASTVisitor v, Object arg) throws PLCException {
		return v.visitDimension(this, arg);
	}


	public Expr getWidth() {
		return width;
	}

	public Expr getHeight() {
		return height;
	}

	public void setWidth(Integer _w) {
		this.w = _w;
	}

	public void setHeight(Integer _h) {
		this.h = _h;
	}

	@Override
	public String toString() {
		return "Dimension [width=" + width + ", height=" + height + "]";
	}

}
