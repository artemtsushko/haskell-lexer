package com.tsushko.spos.hs.lexer;

/**
 * Defines the exception that is thrown when once an unexpected symbol is met
 *
 * @author Artem Tsushko
 * @version 1.0
 */
public class UnexpectedSymbolException extends Exception {
    public UnexpectedSymbolException() {}

    public UnexpectedSymbolException(String msg) {
        super(msg);
    }

    public UnexpectedSymbolException(String msg, Exception cause) {
        super(msg,cause);
    }

    public UnexpectedSymbolException(char msg) {
        super(String.valueOf(msg));
    }

    public UnexpectedSymbolException(char msg, Exception cause) {
        super(String.valueOf(msg),cause);
    }

    public UnexpectedSymbolException(Exception cause) {
        super(cause);
    }
}
