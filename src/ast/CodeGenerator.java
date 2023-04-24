package edu.ufl.cise.plcsp23.ast;

import edu.ufl.cise.plcsp23.IToken;
import edu.ufl.cise.plcsp23.PLCException;
import edu.ufl.cise.plcsp23.TypeCheckException;

import javax.naming.Name;
import javax.swing.plaf.nimbus.State;
import java.util.*;

public class CodeGenerator implements ASTVisitor {
    Boolean write = false;  //import statements
    Boolean math = false;
    Type progType = null;
    int tabTracker = 2;   //formatting
    HashSet<IToken.Kind> boolOps = new HashSet<>(Arrays.asList(IToken.Kind.OR, IToken.Kind.AND, IToken.Kind.LT, IToken.Kind.LE, IToken.Kind.GT, IToken.Kind.GE, IToken.Kind.EQ));

    Set<String> names = new HashSet<String>();
    Stack<Integer> scope = new Stack<Integer>();

    int scopeNum = 1;

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCException {
        progType = program.getType();
        String javaCode = "";
        javaCode += "public class " //public class
                            + program.getIdent().getName() //NAME
                            + " {\n\t"                      //{
                            +"public static "              // public static
                            + getString(program.getType()) + " apply("; //TYPE apply(

        //concat params w string
        List<NameDef> p = program.getParamList();
        for(int i = 0; i < p.size(); i++) {
            javaCode += getString(p.get(i).getType()) + " " + p.get(i).getIdent().getName();
            names.add(p.get(i).getIdent().getName());
            if(i != p.size()-1) {
                javaCode += ", ";
            }
        }
        javaCode += ") { \n";
        String block = (String) program.getBlock().visit(this, arg);
        javaCode += block;
        javaCode += "\t";
        javaCode += "}\n}";

        if(write) {
            javaCode = "import edu.ufl.cise.plcsp23.runtime.ConsoleIO;\n" + javaCode;
        }
        if(math) {
            javaCode = "import java.lang.Math;\n" + javaCode;
        }

        return javaCode;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCException {
        scope.push(scopeNum);
        scopeNum++;
        List<Declaration> decList = block.getDecList();
        List<Statement> stateList = block.getStatementList();

        String blockStr = "";

        for(Declaration d : decList) {
            for(int i = 0; i < tabTracker; i++) {
                blockStr += "\t";
            }
            blockStr += d.visit(this, arg);
        }
        for(Statement s : stateList) {
            for(int i = 0; i < tabTracker; i++) {
                blockStr += "\t";
            }
            blockStr += s.visit(this, arg);
        }

        scope.pop();
        return blockStr;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {
        NameDef nameDef = declaration.getNameDef();
        Expr exp = declaration.getInitializer();

        String decStr = (String)nameDef.visit(this, arg);

        if (exp != null) {
            decStr += " = ";

            if ((nameDef.getType() == Type.STRING && exp.getType() == Type.INT)){
                decStr += "Integer.toString(";
                decStr += exp.visit(this, arg);
                decStr += ")";
            }
            else{
                if(exp.getClass() == BinaryExpr.class) {
                    IToken.Kind op = ((BinaryExpr) exp).getOp();
                    if(nameDef.getType() == Type.INT && (op == IToken.Kind.OR || op == IToken.Kind.AND || op == IToken.Kind.LT || op == IToken.Kind.GT || op == IToken.Kind.LE || op == IToken.Kind.GE || op == IToken.Kind.EQ)) {
                        decStr += "(" + exp.visit(this, arg) + " == true ? 1 : 0)";
                    }
                    else {
                        decStr += exp.visit(this, arg);
                    }
                }
                else {
                    decStr += exp.visit(this, arg);
                }
            }
        }

        decStr += ";\n";
        return decStr;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
        String assignStr = "";
        LValue LV = statementAssign.getLv();
        Expr E = statementAssign.getE();

        assignStr += LV.visit(this, arg);
        assignStr += " = ";
        if ((LV.getIdent().getDef().getType() == Type.STRING && E.getType() == Type.INT)){
            assignStr += "Integer.toString(";
            assignStr += E.visit(this, arg);
            assignStr += ")";
        }
        else {
            if(E.getClass() == BinaryExpr.class) {
                IToken.Kind op = ((BinaryExpr) E).getOp();
                if(LV.getIdent().getDef().getType() == Type.INT && (op == IToken.Kind.OR || op == IToken.Kind.AND || op == IToken.Kind.LT || op == IToken.Kind.GT || op == IToken.Kind.LE || op == IToken.Kind.GE || op == IToken.Kind.EQ)) {
                    assignStr += "(" + E.visit(this, arg) + " == true ? 1 : 0)";
                    assignStr += ";\n";
                    return assignStr;
                }
            }
            assignStr += E.visit(this, arg);
        }

        assignStr += ";\n";
        return assignStr;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCException {
        String retStr = "return ";
        Expr exp = returnStatement.getE();

        if(returnStatement.getE().getType() == Type.INT && progType == Type.STRING) {
            retStr += "Integer.toString(";
            retStr += returnStatement.getE().visit(this, arg);
            retStr += ")";
        }
        else {
            if(exp.getClass() == BinaryExpr.class) {
                IToken.Kind op = ((BinaryExpr) exp).getOp();
                if((op == IToken.Kind.OR || op == IToken.Kind.AND || op == IToken.Kind.LT || op == IToken.Kind.GT || op == IToken.Kind.LE || op == IToken.Kind.GE || op == IToken.Kind.EQ)) {
                    retStr += "(" + exp.visit(this, arg) + " == true ? 1 : 0)";
                    retStr += ";\n";
                    return retStr;
                }
            }
            retStr += returnStatement.getE().visit(this, arg);
        }

        retStr += ";\n";
        return retStr;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCException {
        return Integer.toString(numLitExpr.getValue());
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCException {
        String name = nameDef.getIdent().getName() + "_" + Integer.toString(scope.peek());
        names.add(name);
        String defStr = getString(nameDef.getType());
        defStr += " " + name;
        return defStr;
    }

    @Override
    public Object visitRandomExpr(RandomExpr randomExpr, Object arg) throws PLCException {
        math = true;
        return "(int)Math.floor(Math.random() * 256)";
    }


    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
        String litStr = "\"" + stringLitExpr.getValue() + "\"";
        return litStr;
    }

    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException {
        return Integer.toString(255);
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {
        return lValue.getIdent().visit(this, arg);
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLCException {
        String name = getVarName(ident.getName());
        if(name == null) {
            throw new TypeCheckException("idek wtf is going on");
        }
        return name;
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {
        String name = getVarName(identExpr.getName());
        if(name == null) {
            throw new TypeCheckException("idek wtf is going on");
        }
        return name;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        Expr exp0 = binaryExpr.getLeft();
        IToken.Kind op = binaryExpr.getOp();
        Expr exp1 = binaryExpr.getRight();
        String binStr = "(";

        if(op == IToken.Kind.EXP) {
            math = true;
            binStr = "(int)Math.pow(";
            binStr += exp0.visit(this, arg) + ", ";
            binStr += exp1.visit(this, arg) + ")";
            return binStr;
        }


        if(exp0.getClass() != BinaryExpr.class && (op == IToken.Kind.OR || op == IToken.Kind.AND)) {
            binStr += "(" + exp0.visit(this, arg) + " != 0 ? true : false)";
        }
        else if(exp0.getClass() == BinaryExpr.class) {
            IToken.Kind leftOp = ((BinaryExpr) exp0).getOp();

            //left operand has boolean op but parent expression is non bool
            //ex (i0 > 0 || i1 > 0) * 5
            if(boolOps.contains(leftOp)) {
                if(!(op == IToken.Kind.OR || op == IToken.Kind.AND)) {
                    binStr += "(" + exp0.visit(this, arg) + " ? 1 : 0) ";
                }
                else {
                    binStr += (String)exp0.visit(this, arg);
                }
            }
            //inverse of above
            else {
                if((op == IToken.Kind.OR || op == IToken.Kind.AND)) {
                    binStr += "(" + exp0.visit(this, arg) + " != 0 ? true : false)";
                }
                else {
                    binStr += (String)exp0.visit(this, arg);
                }
            }
        }
        else {
            binStr += (String)exp0.visit(this, arg);
        }

        switch(op) {
            case BITOR -> {
                binStr += " | ";
            }
            case BITAND -> {
                binStr += " & ";
            }
            case OR -> {
                binStr += " || ";
            }
            case AND -> {
                binStr += " && ";
            }
            case LT -> {
                binStr += " < ";
            }
            case GT -> {
                binStr += " > ";
            }
            case LE -> {
                binStr += " <= ";
            }
            case GE -> {
                binStr += " >= ";
            }
            case EQ -> {
                binStr += " == ";
            }
            case PLUS -> {
                binStr += " + ";
            }
            case MINUS -> {
                binStr += " - ";
            }
            case TIMES -> {
                binStr += " * ";
            }
            case DIV -> {
                binStr += " / ";
            }
            case MOD -> {
                binStr += " % ";
            }
        }

        if(exp1.getClass() != BinaryExpr.class && (op == IToken.Kind.OR || op == IToken.Kind.AND)) {
            binStr += exp1.visit(this, arg) + " != 0 ? true : false";
        }
        else {
            binStr += (String)exp1.visit(this, arg);
        }


        binStr += ")";
        return binStr;
    }

    @Override
    public Object visitWriteStatement(WriteStatement statementWrite, Object arg) throws PLCException {
        write = true;
        String writeStr = "ConsoleIO.write(";
        writeStr += statementWrite.getE().visit(this, arg) + ");\n";
        return writeStr;
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException {
        IToken.Kind op = unaryExpr.getOp();
        String unaryStr = "";

        switch (op) {
            case BANG -> {
                unaryStr += "!(";
            }
            case MINUS -> {
                unaryStr += "-(";
            }
            case RES_sin -> {
                math = true;
                unaryStr += "Math.sin(";
            }
            case RES_cos -> {
                math = true;
                unaryStr += "Math.cos(";
            }
            case RES_atan -> {
                math = true;
                unaryStr += "Math.atan(";
            }
        }
        unaryStr += unaryExpr.getE().visit(this, arg);
        unaryStr += ")";
        return unaryStr;
    }

    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException {
        String whileStr = "while";
        Expr guard = whileStatement.getGuard();

        Set<String> before = names;

        //singlular ident
        if(guard.getClass() == IdentExpr.class){
            whileStr += "((" + guard.visit(this, arg) +  " != 0 ? true: false))";
        }
        //binary expression
        else if (guard.getClass() == BinaryExpr.class){
            IToken.Kind op = ((BinaryExpr) guard).getOp();

            if (op == IToken.Kind.BITOR || op == IToken.Kind.BITAND || op == IToken.Kind.PLUS || op == IToken.Kind.MINUS|| op == IToken.Kind.TIMES || op == IToken.Kind.DIV || op == IToken.Kind.MOD){
                whileStr += "((" + guard.visit(this, arg) +  "!= 0 ? true: false))";

            }
            else{
                whileStr += "(" + guard.visit(this, arg) + ")";
            }
        }
        else {
            whileStr += "(" + guard.visit(this, arg) + ")";
        }
        whileStr +=  "{\n";


        tabTracker++;
        whileStr += whileStatement.getBlock().visit(this, arg);
        tabTracker--;
        for(int i = 0; i < tabTracker; i++) {
            whileStr += "\t";
        }
        whileStr += "}\n";

        return whileStr;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {
        String condStr = "";

        Expr guard = conditionalExpr.getGuard();
        Expr trueCase = conditionalExpr.getTrueCase();
        Expr falseCase = conditionalExpr.getFalseCase();

        if(guard.getClass() == IdentExpr.class || guard.getClass() == NumLitExpr.class){
            condStr += "((" + guard.visit(this, arg) +  "!= 0 ? true: false) ? " + trueCase.visit(this, arg) + " : " + falseCase.visit(this, arg) + ")";
        }
        else if (guard.getClass() == BinaryExpr.class){
            IToken.Kind op = ((BinaryExpr) guard).getOp();

            if (op == IToken.Kind.BITOR || op == IToken.Kind.BITAND || op == IToken.Kind.PLUS || op == IToken.Kind.MINUS|| op == IToken.Kind.TIMES || op == IToken.Kind.DIV || op == IToken.Kind.MOD){
                condStr += "((" + guard.visit(this, arg) +  "!= 0 ? true: false) ? " + trueCase.visit(this, arg) + " : " + falseCase.visit(this, arg) + ")";

            }
            else{
                condStr += "((" + guard.visit(this, arg) + ") ? " + trueCase.visit(this, arg) + " : " + falseCase.visit(this, arg) + ")";
            }
        }
        return condStr;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
        return null;
    }

    public String getString(Type tp) {
        if(tp == Type.STRING) {
            return "String";
        }
        return tp.toString().toLowerCase();
    }

    public String getVarName(String name) {

        for(int i = scope.peek(); i > 0; i--) {
            String n = name + "_" + Integer.toString(i);
            if(names.contains(n)) {
                return n;
            }
        }
        if(names.contains(name)) {
            return name;
        }

        return null;
    }

}
