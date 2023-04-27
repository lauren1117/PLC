package edu.ufl.cise.plcsp23.ast;

import edu.ufl.cise.plcsp23.IToken;
import edu.ufl.cise.plcsp23.PLCException;
import edu.ufl.cise.plcsp23.TypeCheckException;

import javax.naming.Name;
import javax.swing.plaf.nimbus.State;
import java.awt.image.BufferedImage;
import java.util.*;

public class CodeGenerator implements ASTVisitor {
    Boolean write = false;  //import statements
    Boolean math = false;
    Boolean imgOp = false;
    Boolean fileURL = false;
    Boolean pixelOp = false;
    Boolean buffImage = false;

    Type progType = null;
    int tabTracker = 2;   //formatting
    HashSet<IToken.Kind> boolOps = new HashSet<>(Arrays.asList(IToken.Kind.OR, IToken.Kind.AND, IToken.Kind.LT, IToken.Kind.LE, IToken.Kind.GT, IToken.Kind.GE, IToken.Kind.EQ));
    HashSet<IToken.Kind> imgOps = new HashSet<>(Arrays.asList(IToken.Kind.PLUS, IToken.Kind.MINUS, IToken.Kind.TIMES, IToken.Kind.DIV, IToken.Kind.MOD));

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
        if(imgOp) {
            javaCode = "import edu.ufl.cise.plcsp23.runtime.ImageOps;\n" + javaCode;
        }
        if(fileURL) {
            javaCode = "import edu.ufl.cise.plcsp23.runtime.FileURLIO;\n" + javaCode;
        }
        if(pixelOp) {
            javaCode = "import edu.ufl.cise.plcsp23.runtime.PixelOps;\n" + javaCode;
        }
        if(buffImage) {
            javaCode = "import java.awt.image.BufferedImage;\n" + javaCode;
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

        removeVars();
        scope.pop();
        return blockStr;
    }

    @Override
    public Object visitDeclaration(Declaration declaration, Object arg) throws PLCException {
        NameDef nameDef = declaration.getNameDef();
        Expr exp = declaration.getInitializer();
        String dimStr = null;

        String decStr = (String)nameDef.visit(this, arg);


        //initializer exists
        if (exp != null) {
            decStr += " = ";

            if ((nameDef.getType() == Type.STRING && exp.getType() == Type.INT)){
                decStr += "Integer.toString(";
                decStr += exp.visit(this, arg);
                if(exp.getClass() == UnaryExpr.class) {
                    IToken.Kind op = ((UnaryExpr) exp).getOp();
                    if(op == IToken.Kind.BANG) {
                        decStr += " ? 1 : 0";
                    }
                }
                if(exp.getClass() == BinaryExpr.class) {
                    IToken.Kind op = ((BinaryExpr) exp).getOp();
                    if(boolOps.contains(op)) {
                        decStr += " ? 1 : 0";
                    }
                }
                decStr += ")";
            }

            else if(nameDef.getType() == Type.IMAGE) {
                buffImage = true;
                if(nameDef.getDimension() == null) {
                    if (exp.getType() == Type.STRING) {
                        fileURL = true;
                        decStr += "FileURLIO.readImage(" + exp.visit(this, arg) + ")";
                    }
                    else if (exp.getType() == Type.IMAGE) {
                        imgOp = true;
                        decStr += "ImageOps.cloneImage(" + exp.visit(this, arg) + ")";
                    }
                }
                else {
                    if (exp.getType() == Type.STRING) {
                        fileURL = true;
                        decStr += "FileURLIO.readImage(" + exp.visit(this, arg) + ", " + nameDef.getDimension().getWidth().visit(this, arg) + ", " + nameDef.getDimension().getHeight().visit(this, arg) + ")";
                    }
                    else if (exp.getType() == Type.IMAGE) {
                        imgOp = true;
                        decStr += "ImageOps.copyAndResize(" + exp.visit(this, arg) + ", " + nameDef.getDimension().getWidth().visit(this, arg) + ", " + nameDef.getDimension().getHeight().visit(this, arg) + ")";
                    }
                    else {
                        imgOp = true;
                        decStr += "ImageOps.makeImage(" + nameDef.getDimension().getWidth().visit(this, arg) + ", " + nameDef.getDimension().getHeight().visit(this, arg) + ");\n";
                        for(int i = 0; i < tabTracker; i++) {
                            decStr += "\t";
                        }
                        decStr += "ImageOps.setAllPixels(" + nameDef.getIdent().visit(this, arg) + ", " + exp.visit(this, arg) + ")";
                    }
                }
            }

            else {
                if(exp.getClass() == BinaryExpr.class) {
                    IToken.Kind op = ((BinaryExpr) exp).getOp();
                    if(nameDef.getType() == Type.INT && (op == IToken.Kind.OR || op == IToken.Kind.AND || op == IToken.Kind.LT || op == IToken.Kind.GT || op == IToken.Kind.LE || op == IToken.Kind.GE || op == IToken.Kind.EQ)) {
                        decStr += "(" + exp.visit(this, arg) + " == true ? 1 : 0)";
                    }
                    else {
                        decStr += exp.visit(this, arg);
                    }
                }
                else if(exp.getClass() == UnaryExpr.class) {
                    IToken.Kind op = ((UnaryExpr) exp).getOp();
                    if(declaration.getNameDef().getType() == Type.INT && op == IToken.Kind.BANG) {
                        decStr += "(" + exp.visit(this, arg) + " == true ? 1 : 0)";
                        decStr += ";\n";
                        return decStr;
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

        else if(nameDef.getType() == Type.IMAGE) {
            buffImage = true;
            if(nameDef.getDimension() == null ) {
                throw new TypeCheckException("Image without initializer requires a dimension");
            }
            else {
                imgOp = true;
                decStr += " = ImageOps.makeImage(" + nameDef.getDimension().getWidth().visit(this, arg) + ", " + nameDef.getDimension().getHeight().visit(this, arg) + ")";
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
        String idName = (String) LV.getIdent().visit(this, arg);


        assignStr += LV.visit(this, arg);
        assignStr += " = ";
        if ((LV.getIdent().getDef().getType() == Type.STRING && E.getType() == Type.INT)){
            assignStr += "Integer.toString(";
            assignStr += E.visit(this, arg);
            if(E.getClass() == UnaryExpr.class) {
                IToken.Kind op = ((UnaryExpr) E).getOp();
                if(op == IToken.Kind.BANG) {
                    assignStr += " ? 1 : 0";
                }
            }
            assignStr += ")";
        }

        else if(LV.getIdent().getDef().getType() == Type.IMAGE) {
            imgOp = true;
            fileURL = true;

            String ht = "";
            String wt = "";
            if(LV.getIdent().getDef().getDimension() != null) {
                ht = (String) LV.getIdent().getDef().getDimension().getHeight().visit(this, arg);
                wt = (String) LV.getIdent().getDef().getDimension().getWidth().visit(this, arg);
            }

            if(LV.getPixelSelector() == null) {
                if(E.getType() == Type.STRING) {
                    assignStr = "ImageOps.copyInto(FileURLIO.readImage(" + E.visit(this, arg) + "), " + LV.getIdent().visit(this, arg) + ")";
                }
                else if(E.getType() == Type.IMAGE) {
                    assignStr = "ImageOps.copyInto(" + E.visit(this, arg) + ", " + LV.getIdent().visit(this, arg) + ")";
                }
                else if(E.getType() == Type.PIXEL) {
                    assignStr = "ImageOps.setAllPixels(" + LV.getIdent().visit(this, arg) + ", " + E.visit(this, arg) + ")";
                }
            }
            //pixel selector not empty
            else {
                assignStr = "for(int y = 0; y < " + idName + ".getHeight(); y++) {\n";
                for(int i = 0; i < tabTracker + 1; i++) {
                    assignStr += "\t";
                }
                assignStr += "for(int x = 0; x < " + idName + ".getWidth(); x++) {\n";
                for(int i = 0; i < tabTracker + 2; i++) {
                    assignStr += "\t";
                }

                //color channel empty
                if(LV.getColor() == null) {
                    assignStr += "ImageOps.setRGB(" + idName + ", " + wt + ", " + ht + ", " + E.visit(this, arg) + ");\n";
                }
                //color channel not empty
                else {
                    pixelOp = true;
                    String color = LV.getColor().name().substring(0, 1).toUpperCase() + LV.getColor().name().substring(1);
                    assignStr += "ImageOps.setRGB(" + idName + ", " + wt + ", " + ht;
                    assignStr += ", PixelOps.set" + color + "(ImageOps.getRGB(" + idName + ", " + wt + ", " + ht + "), ";
                    assignStr += E.visit(this, arg) + "));\n";
                }
                for(int i = 0; i < tabTracker + 1; i++) {
                    assignStr += "\t";
                }
                assignStr += "}\n";
                for(int i = 0; i < tabTracker; i++) {
                    assignStr += "\t";
                }
                assignStr += "}\n";
                return assignStr;
            }
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
            else if(E.getClass() == UnaryExpr.class) {
                IToken.Kind op = ((UnaryExpr) E).getOp();
                if(LV.getIdent().getDef().getType() == Type.INT && op == IToken.Kind.BANG) {
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

        //operator is exponent
        if(op == IToken.Kind.EXP) {
            math = true;
            binStr = "(int)Math.pow(";
            binStr += exp0.visit(this, arg) + ", ";
            binStr += exp1.visit(this, arg) + ")";
            return binStr;
        }

        //if operator is valid for image
        if(imgOps.contains(op)) {
            imgOp = true;
            //lhs is image
            if (exp0.getType() == Type.IMAGE) {
                //rhs is image
                if (exp1.getType() == Type.IMAGE) {
                    binStr += "ImageOps.binaryImageImageOp(ImageOps.OP." + op.name() + ", " + binaryExpr.getLeft().visit(this,arg) + ", " + binaryExpr.getRight().visit(this, arg) + "))";
                }
                //lhs is int
                else if (exp1.getType() == Type.INT) {
                    binStr += "ImageOps.binaryImageScalarOp(ImageOps.OP." + op.name() + ", " + binaryExpr.getLeft().visit(this,arg) + ", " + binaryExpr.getRight().visit(this, arg) + "))";
                }
                return binStr;
            }

            //lhs and rhs are pixel
            else if(exp0.getType() == Type.PIXEL) {
                if(exp1.getType() == Type.PIXEL) {
                    binStr += "ImageOps.binaryPackedPixelPixelOp(ImageOps.OP." + op.name() + ", " + binaryExpr.getLeft().visit(this, arg) + ", " + binaryExpr.getRight().visit(this, arg) + "))";
                }
                else if(exp1.getType() == Type.INT) {
                    binStr += "ImageOps.binaryPackedPixelIntOp(ImageOps.OP." + op.name() + ", " + binaryExpr.getLeft().visit(this, arg) + ", " + binaryExpr.getRight().visit(this, arg) + "))";
                }
                return binStr;
            }
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

        binStr += getOpString(op);

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
        if(op == IToken.Kind.BANG && unaryExpr.getE().getType() == Type.INT) {
            unaryStr += "== 0 ? false : true";
        }
        unaryStr += ")";
        return unaryStr;
    }

    @Override
    public Object visitUnaryExprPostFix(UnaryExprPostfix unaryExprPostfix, Object arg) throws PLCException {
        String unaryStr = "";
        if(unaryExprPostfix.getPrimary().getType() == Type.IMAGE) {
            imgOp = true;
            if(unaryExprPostfix.getPixel() != null) {
                if(unaryExprPostfix.getColor() == null) {
                    unaryStr += "ImageOps.getRGB(" + unaryExprPostfix.getPrimary().visit(this,arg) + ", " + unaryExprPostfix.getPixel().visit(this, arg) + ")";
                }
                else {
                    pixelOp = true;
                    unaryStr += "PixelOps." + unaryExprPostfix.getColor().name() + "(" + "ImageOps.getRGB(" + unaryExprPostfix.getPrimary().visit(this,arg) + ", " + unaryExprPostfix.getPixel().visit(this, arg) + ")" + ")";
                }
            }
            else {
                if(unaryExprPostfix.getColor() != null) {
                    unaryStr += "ImageOps.extract";
                    String color = unaryExprPostfix.getColor().name().substring(0, 1).toUpperCase() + unaryExprPostfix.getColor().name().substring(1);
                    unaryStr += color + "(" + unaryExprPostfix.getPrimary().visit(this, arg) + ")";
                }
            }
        }
        else if(unaryExprPostfix.getPrimary().getType() == Type.PIXEL) {
            if(unaryExprPostfix.getColor() != null) {
                pixelOp = true;
                unaryStr += "PixelOps." + unaryExprPostfix.getColor().name() + "(" + unaryExprPostfix.getPrimary().visit(this, arg) + ")";
            }
        }
        return unaryStr;
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

            if (op == IToken.Kind.BITOR || op == IToken.Kind.BITAND || op == IToken.Kind.PLUS || op == IToken.Kind.MINUS|| op == IToken.Kind.TIMES || op == IToken.Kind.DIV || op == IToken.Kind.MOD || op == IToken.Kind.EXP){
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

        if(guard.getClass() == IdentExpr.class || guard.getClass() == NumLitExpr.class || guard.getClass() == ZExpr.class){
            condStr += "((" + guard.visit(this, arg) +  "!= 0 ? true: false) ? " + trueCase.visit(this, arg) + " : " + falseCase.visit(this, arg) + ")";
        }
        else if (guard.getClass() == BinaryExpr.class){
            IToken.Kind op = ((BinaryExpr) guard).getOp();

            if (op == IToken.Kind.BITOR || op == IToken.Kind.BITAND || op == IToken.Kind.PLUS || op == IToken.Kind.MINUS|| op == IToken.Kind.TIMES || op == IToken.Kind.DIV || op == IToken.Kind.MOD || op == IToken.Kind.EXP){
                condStr += "((" + guard.visit(this, arg) +  "!= 0 ? true: false) ? " + trueCase.visit(this, arg) + " : " + falseCase.visit(this, arg) + ")";

            }
            else{
                condStr += "((" + guard.visit(this, arg) + ") ? " + trueCase.visit(this, arg) + " : " + falseCase.visit(this, arg) + ")";
            }
        }
        else {
            condStr += "((" + guard.visit(this, arg) + ") ? " + trueCase.visit(this, arg) + " : " + falseCase.visit(this, arg) + ")";
        }
        return condStr;
    }

    @Override
    public Object visitDimension(Dimension dimension, Object arg) throws PLCException {
        String dim = (String)dimension.getWidth().visit(this, arg);
        dim += "," + dimension.getHeight().visit(this, arg);
        return dim;
    }

    @Override
    public Object visitExpandedPixelExpr(ExpandedPixelExpr expandedPixelExpr, Object arg) throws PLCException {
        pixelOp = true;
        String pix = "PixelOps.pack(";
        pix += expandedPixelExpr.getRedExpr().visit(this, arg) + ", ";
        pix += expandedPixelExpr.getGrnExpr().visit(this, arg) + ", ";
        pix += expandedPixelExpr.getBluExpr().visit(this, arg) + ")";
        return pix;
    }

    @Override
    public Object visitPixelFuncExpr(PixelFuncExpr pixelFuncExpr, Object arg) throws PLCException {
        return null;
    }

    @Override
    public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws PLCException {
        return pixelSelector.getX().visit(this, arg) + ", " + pixelSelector.getY().visit(this, arg);
    }

    @Override
    public Object visitPredeclaredVarExpr(PredeclaredVarExpr predeclaredVarExpr, Object arg) throws PLCException {
        return predeclaredVarExpr.firstToken.getTokenString();
    }

    public String getString(Type tp) {
        if(tp == Type.STRING) {
            return "String";
        }
        else if(tp == Type.PIXEL) {
            return "int";
        }
        else if(tp == Type.IMAGE) {
            return "BufferedImage";
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

    public void removeVars() {
        Iterator<String> namesIterator = names.iterator();
        ArrayList<String> arrNames = new ArrayList<String>();

        while(namesIterator.hasNext()) {
            arrNames.add(namesIterator.next());
        }
        for(String name : arrNames) {
            String last2char = name.length() > 2 ? name.substring(name.length() - 2) : name;
            if(last2char.equals("_" + Integer.toString(scope.peek()))) {
                names.remove(name);
            }
        }
    }

    public String getOpString(IToken.Kind k) {
        switch(k) {
            case BITOR -> {
                return " | ";
            }
            case BITAND -> {
                return " & ";
            }
            case OR -> {
                return " || ";
            }
            case AND -> {
                return " && ";
            }
            case LT -> {
                return " < ";
            }
            case GT -> {
                return " > ";
            }
            case LE -> {
                return " <= ";
            }
            case GE -> {
                return " >= ";
            }
            case EQ -> {
                return " == ";
            }
            case PLUS -> {
                return " + ";
            }
            case MINUS -> {
                return " - ";
            }
            case TIMES -> {
                return " * ";
            }
            case DIV -> {
                return " / ";
            }
            case MOD -> {
                return " % ";
            }
        }
        return "";
    }

}
