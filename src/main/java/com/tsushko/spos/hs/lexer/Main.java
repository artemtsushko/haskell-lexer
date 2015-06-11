package com.tsushko.spos.hs.lexer;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Lexes the Haskell program file and prints tokens to std output.
 * The input file path must be passed as command line argument.
 *
 * @author Artem Tsushko
 * @version 1.0
 */
public class Main {
    public static void main(String[] args)
            throws IOException, UnexpectedSymbolException {
        Lexer lexer = new Lexer();
        File file = new File(args);
        List<Token> tokens = lexer.lexDocument(file);
        for (Token token : tokens) {
            System.out.println(token);
        }
    }
}
