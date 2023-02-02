public class MyToken implements edu.ufl.cise.plcsp23.IToken {
    String tokenString;
    Kind tokenKind;
    SourceLocation sourceLocation;

    @Override
    public SourceLocation getSourceLocation() {
        return null;
    }

    @Override
    public Kind getKind() {
        return null;
    }

    @Override
    public String getTokenString() {
        return "token_string";
    }
}
