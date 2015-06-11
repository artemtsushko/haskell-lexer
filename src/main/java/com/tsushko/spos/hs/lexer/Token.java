package com.tsushko.spos.hs.lexer;

/**
 * Represents a token as pair {type, string value}
 *
 * @author Artem Tsushko
 * @version 1.0
 */
public class Token {

    private Type type;
    private String string;

    public static enum Type {
        numericConstant,
        symbolicConstant,
        comment,
        keyword,
        pragma,
        identifier,
        operator,
        punctuation,
        unidentified
    };

    public Token(Type type, String string) {
        this.type = type;
        this.string = string;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getString() {
        return string;
    }

    public void setString(String string) {
        this.string = string;
    }

    @Override
    public String toString() {
        return "[" + type + ", \"" + string + "\"]";
    }
}
