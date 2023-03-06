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

public class CompilerComponentFactory {
	public static IScanner makeScanner(String input) throws LexicalException {
		//Add statement to return an instance of your scanner
		return new MyScanner(input);
	}
	public static IParser makeAssignment2Parser(String input) throws PLCException {
		//add code to create a scanner and parser and return the parser.
		return new MyParser(input);
	}

	public static IParser makeParser(String input) throws PLCException {
<<<<<<< HEAD
		//add code to create a scanner and parser and return the parser.
=======
>>>>>>> 9cb547f796f7b583876e6fa2220cb4976220f028
		return new MyParser(input);
	}

}
