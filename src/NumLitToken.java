package edu.ufl.cise.plcsp23;

class NumLitToken implements INumLitToken {

    final int value;
    final SourceLocation sourceLocation;
    final Kind kind;
    final String token;

    public NumLitToken(int val, SourceLocation sl, Kind tokKind, String tokStr){
        value = val;
        sourceLocation = sl;
        kind = tokKind;
        token = tokStr;
    }

    public int getValue() {
        return value;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public String getTokenString() {
        return token;
    }
}
