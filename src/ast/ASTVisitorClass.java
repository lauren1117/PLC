package edu.ufl.cise.plcsp23.ast;

import edu.ufl.cise.plcsp23.PLCException;
import edu.ufl.cise.plcsp23.TypeCheckException;
import edu.ufl.cise.plcsp23.ast.*;

import javax.naming.Name;
import java.util.HashMap;
import java.util.List;

public class ASTVisitorClass implements ASTVisitor {

    //Symbol table class for keeping track of scope
    public class SymbolTable {
        HashMap<String, Declaration> entries = new HashMap<>();

        //returns true if successfully inserted, false if it was already there
        public boolean insert(String name, Declaration dec) {
            return (entries.putIfAbsent(name, dec) == null);
        }

        public Declaration lookup(String name) {
            return entries.get(name);
        }
    }

    SymbolTable table = new SymbolTable();

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCException {
        List<NameDef> paramList = program.getParamList();
        if(paramList != null) {
            for(NameDef n : paramList) {
                n.visit(this, arg);
            }
        }
        program.getBlock().visit(this, arg);
        return null;
    }

    @Override
    public Object visitBlock(Block block, Object arg) throws PLCException {
        List<Declaration> decList = block.getDecList();
        List<Statement> stateList = block.getStatementList();

        for(Declaration d : decList) {
            d.visit(this, arg);
        }
        for(Statement s : stateList) {
            s.visit(this, arg);
        }

        return null;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {
        NameDef nameDef = declaration.getNameDef();
        Expr exp = declaration.getInitializer();
        nameDef.visit(this, arg);
        if (exp != null){
            exp.visit(this, arg);
        }
        if (nameDef.getType() == Type.IMAGE){
            if (exp == null && nameDef.getDimension() == null){
                throw new TypeCheckException("no initialize or dimension even though its an image");
            }
        }

        return null;
    }

    @Override
    public Object visitNameDef(NameDef nameDef, Object arg) throws PLCException {
        if (nameDef.getDimension() != null){
            if (nameDef.getType() != Type.IMAGE){
                throw new TypeCheckException("there is a dimension but the type isn't image");
            }
            nameDef.getDimension().visit(this, arg);
        }
        if(nameDef.getType() == Type.VOID) {
            throw new TypeCheckException("Identifier cannot be of type void");
        }

        return null;
    }

    @Override
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitConditionalExpr(ConditionalExpr conditionalExpr, Object arg) throws PLCException {
        Type expr0 = (Type) conditionalExpr.getGuard().visit(this, arg);
        Type expr1 = (Type) conditionalExpr.getTrueCase().visit(this, arg);
        Type expr2 = (Type) conditionalExpr.getFalseCase().visit(this, arg);

        if (expr0 != Type.INT){
            throw new TypeCheckException("Guard not an INT");
        }
        if (expr1 != expr2) {
            throw new TypeCheckException("True and False case not the same");
        }

        conditionalExpr.setType(expr1);
        return conditionalExpr.getType();
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException {
        unaryExpr.getE().visit(this, arg);
        //see table for allowed operators

        return null;
    }

    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
        stringLitExpr.setType(Type.STRING);
        return stringLitExpr.getType();
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {
        //check if identExpr.getName() is defined and visible in scope
        //get namedef type from symbol table

        identExpr.setType(table.lookup(identExpr.getName()).getNameDef().getType());
        return null;
    }

    @Override
    public Object visitNumLitExpr(NumLitExpr numLitExpr, Object arg) throws PLCException {
        numLitExpr.setType(Type.INT);
        return numLitExpr.getType();
    }

    @Override
    public Object visitZExpr(ZExpr zExpr, Object arg) throws PLCException {
        zExpr.setType(Type.INT);
        return zExpr.getType();
    }

    @Override
    public Object visitRandomExpr(RandomExpr randomExpr, Object arg) throws PLCException {
        randomExpr.setType(Type.INT);
        return randomExpr.getType();
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        Type expr0Type = (Type) dimension.getHeight().visit(this, arg);
        Type expr1Type = (Type) dimension.getWidth().visit(this, arg);

        if (expr0Type != expr1Type){
            throw new TypeCheckException("at least one expr in dimension is not an int");
        }

        return Type.INT;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        Type expr0Type = (Type) expandedPixelExpr.getRedExpr().visit(this, arg);
        Type expr1Type = (Type) expandedPixelExpr.getBluExpr().visit(this, arg);
        Type expr2Type = (Type) expandedPixelExpr.getGrnExpr().visit(this, arg);

        if (expr0Type != Type.INT || expr1Type != Type.INT || expr2Type != Type.INT){
            throw new TypeCheckException("at least one of the expressions in expandedPixelExpr is not an int");
        }

        expandedPixelExpr.setType(Type.PIXEL);
        return expandedPixelExpr.getType();
    }

    @Override
    public Object visitIdent(Ident ident, Object arg) throws PLCException {
        //set type based on type assigned when declared

        return null;
    }

    @Override
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {
        //check if lValue.getIdent() has been declared and is visible in this scope
        //see table (idk for what though)


        return null;
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        //pixel selector is properly typed
        //visit it so that the check of type of pixelFuncExpr.getSelector() happens and it can throw exception if needed
        pixelFuncExpr.getSelector().visit(this, arg);


        pixelFuncExpr.setType(Type.INT);
        return pixelFuncExpr.getType();
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        Type xType = (Type) pixelSelector.getX().visit(this,arg);
        Type yType = (Type) pixelSelector.getY().visit(this, arg);

        if (xType != Type.INT){
            throw new TypeCheckException("pixelSelector.x not an int @ row: " + pixelSelector.getLine() + " col: " + pixelSelector.getColumn() );
        }
        if (yType != Type.INT){
            throw new TypeCheckException("pixelSelector.y not an int @ row: " + pixelSelector.getLine() + " col: " + pixelSelector.getColumn() );
        }
        //pixelSelector does not have a type
        //just return an int type??
        return Type.INT;
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
        predeclaredVarExpr.setType(Type.INT);
        return predeclaredVarExpr.getType();
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCException {
        //get program type in this function, maybe pass in as arg??
        //Type progType = (Type) ;
        //maybe lookup in symbol table??
        Type exprType = (Type) returnStatement.getE().visit(this, arg);

        /*if (exprType != progType){
            throw new TypeCheckException("return value and program type not the same");
        }*/

        return exprType;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
        Type LVType = (Type) statementAssign.getLv().visit(this, arg);
        Type EType = (Type) statementAssign.getE().visit(this, arg);

        //compare LVType and EType based on Assignment Compatibility table


        return null;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException {
        Type guardType = (Type) whileStatement.getGuard().visit(this, arg);
        if (guardType != Type.INT){
            throw new TypeCheckException("while statement guard not an int");
        }

        //enter scope ??
        whileStatement.getBlock().visit(this, arg);
        //exit scope ??


        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement statementWrite, Object arg) throws PLCException {
        statementWrite.getE().visit(this, arg);

        return null;
    }


}
