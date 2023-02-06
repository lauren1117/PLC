package edu.ufl.cise.plcsp23;

public class StringLitToken implements IStringLitToken {
    final String value;
    final SourceLocation sourceLocation;
    final Kind kind;
    final String token;

    public StringLitToken(String val, SourceLocation sl, Kind tokKind, String tokStr){
        value = val;
        sourceLocation = sl;
        kind = tokKind;
        token = tokStr;
    }


    @Override
    public String getValue() {
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
