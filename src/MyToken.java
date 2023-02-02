public class MyToken implements edu.ufl.cise.plcsp23.IToken {
    final String tokenString;
    final Kind tokenKind;
    final SourceLocation sourceLocation;

    public MyToken(String token, Kind kind, SourceLocation location){
        tokenString = token;
        tokenKind = kind;
        sourceLocation = location;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    @Override
    public Kind getKind() {
        return tokenKind;
    }

    @Override
    public String getTokenString() {
        return tokenString;
    }
}
