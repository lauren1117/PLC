package edu.ufl.cise.plcsp23.ast;

import edu.ufl.cise.plcsp23.IToken;
import edu.ufl.cise.plcsp23.PLCException;
import edu.ufl.cise.plcsp23.TypeCheckException;
import edu.ufl.cise.plcsp23.ast.*;

import javax.naming.Name;
import java.util.*;

public class ASTVisitorClass implements ASTVisitor {

    //Symbol table class for keeping track of scope
    public class SymbolTable {

        HashMap<String, NameDef> vars = new HashMap<>();
        HashMap<String, Boolean> definitions = new HashMap<>();
        HashMap<Integer, HashMap<String, NameDef>> scopeVars = new HashMap<>();
        Stack<Integer> scope = new Stack<>();

        int scopeNum = 0;

        //returns true if successfully inserted, false if it was already there
        public boolean insertEntry(String name, NameDef def) {
            if(!vars.containsKey(name)) {
                vars.put(name, def);
            }
            return (scopeVars.get(scope.peek()).putIfAbsent(name, def) == null);
        }

        public boolean insertScope(Integer scope, HashMap<String, NameDef> vars) {
            return(scopeVars.putIfAbsent(scope, vars) == null);
        }

        public void removeVars(Integer s) {
            //iterate through name defs, remove names from entries map
            HashMap<String, NameDef> map = scopeVars.get(s);

            for (Map.Entry<String,NameDef> mapElement : map.entrySet()) {
                String key = mapElement.getKey();
                definitions.remove(key);
                vars.remove(key);
            }

            scopeVars.remove(s);
        }

        public NameDef lookup(String name) {
            NameDef nDef = null;
            for (Map.Entry<Integer, HashMap<String, NameDef>> mapElement : scopeVars.entrySet()) {
                NameDef n = mapElement.getValue().get(name);
                if(n != null) {
                    nDef = n;
                }
            }
            return nDef;
        }
    }
    SymbolTable table = new SymbolTable();
    Type progType = null;

    @Override
    public Object visitProgram(Program program, Object arg) throws PLCException {
        table.scope.push(table.scopeNum);
        table.insertScope(table.scope.peek(), new HashMap<String, NameDef>());

        progType = program.getType();

        List<NameDef> paramList = program.getParamList();
        if(paramList != null) {
            for(NameDef n : paramList) {
                n.visit(this, arg);
                if (!table.insertEntry(n.getIdent().getName(), n)) {
                    throw new TypeCheckException("Attempted redeclaration of IDENT: " + n.getIdent().getName());
                }

                else {
                    table.definitions.put(n.getIdent().getName(), true);
                }
            }
        }
        program.getBlock().visit(this, arg);

        table.removeVars(table.scope.pop());
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
            Type expType = (Type)exp.getType();
            checkAssignTypes(nameDef.getType(), expType);
        }

        if (!table.insertEntry(nameDef.getIdent().getName(), nameDef)) {
            throw new TypeCheckException("Attempted redeclaration of IDENT: " + nameDef.getIdent().getName());
        }

        else {
            table.scopeVars.get(table.scope.peek()).put(nameDef.getIdent().getName(), nameDef);
            Boolean def = declaration.getInitializer() != null;
            table.definitions.put(nameDef.getIdent().getName(), def);
        }
        if (nameDef.getType() == Type.IMAGE){
            if (exp == null && nameDef.getDimension() == null){
                throw new TypeCheckException("Image requires initializer or dimension");
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
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException {
        PixelSelector px = unaryExprPostfix.getPixel();
        ColorChannel ch = unaryExprPostfix.getColor();
        if(px != null) {
            px.visit(this, arg);
        }

        Type primType = (Type) unaryExprPostfix.getPrimary().visit(this, arg);

        if(primType == Type.PIXEL) {
            if(px == null && ch != null) {
                unaryExprPostfix.setType(Type.INT);
            }
            else {
                throw new TypeCheckException("Bad UnaryPostFix for PIXEL type");
            }
        }
        else if(primType == Type.IMAGE) {
            if(px == null && ch != null) {
                unaryExprPostfix.setType(Type.IMAGE);
            }
            else if(px != null) {
                if(ch != null) {
                    unaryExprPostfix.setType(Type.INT);
                }
                else {
                    unaryExprPostfix.setType(Type.PIXEL);
                }
            }
            else {
                throw new TypeCheckException("Expression must have pixel or channel selector");
            }
        }
        else {
            throw new TypeCheckException("primtype not image or pixel");
        }

        return unaryExprPostfix.getType();
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
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
        predeclaredVarExpr.setType(Type.INT);
        return predeclaredVarExpr.getType();
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
    public Object visitBinaryExpr(BinaryExpr binaryExpr, Object arg) throws PLCException {
        Type LType = (Type) binaryExpr.getLeft().visit(this, arg);
        Type RType = (Type) binaryExpr.getRight().visit(this, arg);
        IToken.Kind op = binaryExpr.getOp();

        switch(op) {
            case BITOR, BITAND -> {
                if(LType == Type.PIXEL && RType == Type.PIXEL) {
                    binaryExpr.setType(Type.PIXEL);
                }
                else {
                    throw new TypeCheckException("BITWISE operands must be pixels");
                }
            }
            case OR, AND -> {
                if(LType == Type.INT && RType == Type.INT) {
                    binaryExpr.setType(Type.INT);
                }
                else {
                    throw new TypeCheckException("Logical operands must be pixels");
                }
            }
            case LT, GT, LE, GE -> {
                if(LType == Type.INT && RType == Type.INT) {
                    binaryExpr.setType(Type.INT);
                }
                else {
                    throw new TypeCheckException("Comparison operands must be pixels");
                }
            }
            case EQ -> {
                if(LType == Type.VOID || RType == Type.VOID) {
                    throw new TypeCheckException("EQ operands cannot be of type void");
                }
                if(LType != RType) {
                    throw new TypeCheckException("EQ operands must be of the same type");
                }
                else {
                    binaryExpr.setType(Type.INT);
                }
            }
            case EXP -> {
                if(RType == Type.INT) {
                    if(LType == Type.INT) {
                        binaryExpr.setType(Type.INT);
                    }
                    else if(LType == Type.PIXEL) {
                        binaryExpr.setType(Type.PIXEL);
                    }
                    else {
                        throw new TypeCheckException("Exponent left operand of invalid type");
                    }
                }
                else {
                    throw new TypeCheckException("Exponent right operand of invalid type");
                }
            }
            case PLUS -> {
                if(RType == LType && RType != Type.VOID) {
                    binaryExpr.setType(LType);
                }
                else {
                    throw new TypeCheckException("Cannot add operands of different or void types");
                }
            }
            case MINUS -> {
                if(RType == LType && RType != Type.VOID && RType != Type.STRING) {
                    binaryExpr.setType(LType);
                }
                else {
                    throw new TypeCheckException("Cannot subtract operands of different or void/string types");
                }
            }
            case TIMES, DIV, MOD -> {
                if(LType == Type.INT) {
                    if(RType == Type.INT) {
                        binaryExpr.setType(Type.INT);
                    }
                    else {
                        throw new TypeCheckException("Operands must both be of type INT");
                    }
                }

                else if(LType == Type.PIXEL) {
                    if(RType == Type.PIXEL || RType == Type.INT) {
                        binaryExpr.setType(Type.PIXEL);
                    }
                    else {
                        throw new TypeCheckException("Operand must be pixel or int");
                    }
                }

                else if(LType == Type.IMAGE) {
                    if(RType == Type.IMAGE || RType == Type.INT) {
                        binaryExpr.setType(Type.IMAGE);
                    }
                    else {
                        throw new TypeCheckException("Operand must be image or int");
                    }
                }
                else {
                    throw new TypeCheckException("Invalid type for operands: TIMES, DIV, MOD");
                }
            }
        }

        return binaryExpr.getType();
    }

    @Override
    public Object visitUnaryExpr(UnaryExpr unaryExpr, Object arg) throws PLCException {
        Type exprType = (Type)unaryExpr.getE().visit(this, arg);
        IToken.Kind op = unaryExpr.getOp();

        //see table for allowed operators
        if(op == IToken.Kind.BANG) {
            if(exprType == Type.INT) {
                unaryExpr.setType(Type.INT);
            }
            else if(exprType == Type.PIXEL) {
                unaryExpr.setType(Type.PIXEL);
            }
            else {
                throw new TypeCheckException("Bang can only be applied to INT or PIXEL");
            }
        }
        else if(op == IToken.Kind.MINUS || op == IToken.Kind.RES_cos || op == IToken.Kind.RES_sin || op == IToken.Kind.RES_atan) {
            if(exprType == Type.INT) {
                unaryExpr.setType(Type.INT);
            }
            else {
                throw new TypeCheckException("Other unary ops can only be applied to INT");
            }
        }

        return unaryExpr.getType();
    }

    @Override
    public Object visitStringLitExpr(StringLitExpr stringLitExpr, Object arg) throws PLCException {
        stringLitExpr.setType(Type.STRING);
        return stringLitExpr.getType();
    }

    @Override
    public Object visitIdentExpr(IdentExpr identExpr, Object arg) throws PLCException {
        //check if identExpr.getName() is defined and visible in scope
        String name = identExpr.getName();
        if(!table.definitions.containsKey(name)){
            throw new TypeCheckException("Identifier must be defined before used");
        }

        //get namedef type from symbol table
        identExpr.setType(table.lookup(name).getType());
        return identExpr.getType();
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
        NameDef def = null;
        //set type based on type assigned when declared
        if(table.scopeVars.get(table.scope.peek()).containsKey(ident.getName())) {
            ident.setDef(table.scopeVars.get(table.scope.peek()).get(ident.getName()));
        }
        else {
            ident.setDef(table.vars.get(ident.getName()));
        }

        return null;
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
    public Object visitLValue(LValue lValue, Object arg) throws PLCException {
        Type idType = (Type)lValue.getIdent().visit(this, arg);
        String name = lValue.getIdent().getName();
        PixelSelector p = lValue.getPixelSelector();
        ColorChannel c = lValue.getColor();

        if(p != null) {
            p.visit(this, arg);
        }

        if(!table.definitions.containsKey(lValue.getIdent().getName())) {
            throw new TypeCheckException("Idents must be declared before they are used");
        }
        Type tp = null;
        if(table.scopeVars.get(table.scope.peek()).containsKey(name)) {
            tp = table.scopeVars.get(table.scope.peek()).get(name).getType();        }
        else {
            tp = table.vars.get(name).getType();
        }

        switch(tp) {
            case IMAGE -> {
                if(p == null) {
                    return Type.IMAGE;
                }
                else if(c == null) {
                    return Type.PIXEL;
                }
                else {
                    return Type.INT;
                }
            }
            case PIXEL -> {
                if(p != null) {
                    throw new TypeCheckException("Pixel should not have selector");
                }
                else {
                    if(c == null){
                        return Type.PIXEL;
                    }
                    else {
                        return Type.INT;
                    }
                }
            }
            case STRING -> {
                return Type.STRING;
            }
            case INT -> {
                return Type.INT;
            }
        }

        return null;
    }

    @Override
    public Object visitAssignmentStatement(AssignmentStatement statementAssign, Object arg) throws PLCException {
        Type LVType = (Type) statementAssign.getLv().visit(this, arg);
        Type EType = (Type) statementAssign.getE().visit(this, arg);
        String idName = statementAssign.getLv().getIdent().getName();

        //compare LVType and EType based on Assignment Compatibility table
        checkAssignTypes(LVType, EType);

        if(table.scopeVars.get(table.scope.peek()).containsKey(idName)) {
            table.definitions.put(idName, true);
        }

        return null;
    }

    @Override
    public Object visitWriteStatement(WriteStatement statementWrite, Object arg) throws PLCException {
        statementWrite.getE().visit(this, arg);

        return null;
    }

    @Override
    public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws PLCException {
        Type guardType = (Type) whileStatement.getGuard().visit(this, arg);
        if (guardType != Type.INT){
            throw new TypeCheckException("while statement guard not an int");
        }

        //enter scope
        table.scopeNum++;
        table.scope.push(table.scopeNum);
        table.insertScope(table.scope.peek(), new HashMap<String, NameDef>());
        whileStatement.getBlock().visit(this, arg);

        table.removeVars(table.scope.pop());

        return null;
    }

    @Override
    public Object visitReturnStatement(ReturnStatement returnStatement, Object arg) throws PLCException {
        //get program type in this function, maybe pass in as arg??

        Type exprType = (Type) returnStatement.getE().visit(this, arg);

        if(progType == Type.VOID) {
            throw new TypeCheckException("Void program cannot have return value");
        }
        if(!((exprType == Type.INT || exprType == Type.PIXEL) && ((progType == Type.INT || progType == Type.PIXEL)))) {
            if (exprType != progType) {
                throw new TypeCheckException("Return type does not match program type");
            }
        }

        return exprType;
    }

    public void checkAssignTypes(Type LVType, Type EType) throws TypeCheckException {
        if(LVType == Type.IMAGE) {
            if(EType == Type.INT || EType == Type.VOID) {
                throw new TypeCheckException("Expression resolved to improper type for IMAGE");
            }
        }
        else if(LVType == Type.PIXEL) {
            if(EType == Type.IMAGE || EType == Type.VOID || EType == Type.STRING) {
                throw new TypeCheckException("Expression resolved to improper type for PIXEL");
            }
        }
        else if(LVType == Type.INT) {
            if(EType == Type.IMAGE || EType == Type.VOID || EType == Type.STRING) {
                throw new TypeCheckException("Expression resolved to improper type for INT");
            }
        }
        else if(LVType == Type.STRING) {
            if(EType == Type.VOID) {
                throw new TypeCheckException("Expression resolved to improper type for STRING");
            }
        }
        else {
            throw new TypeCheckException("Invalid LValue Type");
        }
    }

}
