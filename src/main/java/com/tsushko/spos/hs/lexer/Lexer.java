package com.tsushko.spos.hs.lexer;

import java.io.*;
import java.util.*;

/**
 * Provides tools for splitting the text of a Haskell program
 * into tokens
 *
 * @author Artem Tsushko
 * @version 1.0
 * @see Token
 */
public class Lexer {
    private static final int INPUT_LENGTH = 3;
    private char[] input = new char[INPUT_LENGTH];
    private int curInputLength = 0;
    private PushbackReader reader = null;
    private Set<String> reservedOps = new TreeSet<>(Arrays.asList(
            "..", ":", "::", "=", "\\", "|", "<-", "->", "@", "~", "=>"
    ));
    private List<String> specialVarOps = Arrays.asList("-", "!");
    private Set<String> reservedIds = new TreeSet<>(Arrays.asList(
            "_", "case", "class", "data", "default", "deriving",
            "do", "else", "foreign", "if", "import", "in", "infix",
            "infixl", "infixr", "instance", "let", "module",
            "newtype", "of", "then", "type", "where"
    ));
    private Set<String> specialVarIds = new TreeSet<>(Arrays.asList(
            "as", "export", "hiding", "qualified", "safe", "unsafe"
    ));
    private List<Token> result = null;



    public Lexer() {
        // empty
    }

    private boolean init(File file) throws IOException {
        try {
            this.reader = new PushbackReader(new FileReader(file), INPUT_LENGTH);
            read();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        result = new LinkedList<>();
        return true;
    }

    /**
     * reads next {@link #INPUT_LENGTH} characters from file
     */
    private void read() throws IOException {
        curInputLength = reader.read(input);
    }

    /**
     * shifts current input on specified number of characters
     *
     * @param number the number of characters to advance
     * @return the number of new characters read
     */
    private int advance(int number) throws IOException {
        int advance = Math.min(curInputLength,number);
        reader.unread(input,advance,curInputLength - advance);
        int oldInputLength = curInputLength;
        read();
        return curInputLength - (oldInputLength - advance);
    }

    /**
     * compares the given characters to the beginning of input
     */
    private boolean is(char ... chars) {
        if (chars.length > curInputLength)
            return false;
        for(int i = 0; i < chars.length; ++i) {
            if (chars[i] != input[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * checks whether the end of file was reached
     * @return if the end of file was reached
     */
    private boolean isEOF() {
        return curInputLength == -1;
    }

    /**
     * compares the given characters to the beginning of input
     * from specified offset
     */
    private boolean is(int offset, char ... chars) {
        if (chars.length > curInputLength - offset)
            return false;
        for(int i = 0; i < chars.length; ++i) {
            if (chars[i] != input[i + offset]) {
                return false;
            }
        }
        return true;
    }

    /**
     * checks if the given character could be a part of identifier
     * @param symbol a character from input
     * @return if the given character is a possible part of identifier
     */
    private boolean isIdentifier(char symbol) {
        return (Character.isLetterOrDigit(symbol)
                || symbol == '\'' || symbol == '_');
    }

    /**
     * checks if the given character is symbol
     */
    private boolean isSymbol(char c) {
        return (c == ':' || c == '!' || c == '#' || c == '%' || c == '&'
                || c == '*' || c == '.' || c == '/' || c == '?' || c == '@'
                || c == '\\' || c == '-')
        || (!Character.isWhitespace(c) && !Character.isLetterOrDigit(c)
                && c != '(' && c != ')' && c != ',' && c != ';'
                && c != '[' && c != ']' && c != '`' && c != '{'
                && c != '}' && c != '_' && c != '\"' && c != '\'');
    }

    private boolean isOctDigit(char c) {
        return c == '0' || c == '1' || c == '2' || c == '3'
                || c == '4' || c == '5' || c == '6' || c == '7';
    }

    private boolean isHexDigit(char c) {
        return isOctDigit(c) || c == '8' || c == '9'
                || c == 'a' || c == 'b' || c == 'c'
                || c == 'd' || c == 'e' || c == 'f'
                || c == 'A' || c == 'B' || c == 'C'
                || c == 'D' || c == 'E' || c == 'F';
    }

    private boolean isDigit(char c) {
        return  Character.isDigit(c);
    }


    private boolean isAllDashes(String s) {
        for (int i = 0; i < s.length(); i++) {
            if(s.charAt(i) != '-')
                return false;
        }
        return true;
    }



    public List<Token> lexDocument(File file)
            throws IOException, UnexpectedSymbolException {
        // init
        if (!init(file))
            return null;
        while (!isEOF()) {
            lexToken();
        }
        return result;
    }

    /**
     * initial state
     */
    private void lexToken() throws IOException, UnexpectedSymbolException {
        if (Character.isWhitespace(input[0])) {
            lexWhitespace();
        } else if (is('{', '-', '#')) {
            lexPragma();
        } else if (is('{','-')) {
            lexNestedComment();
        } else if (is('-','-')) {
            lexDashes();
        } else if (is('0')
                && curInputLength > 2
                && Character.toLowerCase(input[1]) == 'o'
                && isOctDigit(input[2])) {
            lexOctDigit();
        } else if (is('0')
                && curInputLength > 2
                && Character.toLowerCase(input[1]) == 'x'
                && isHexDigit(input[2])) {
            lexHexDigit();
        } else if (isDigit(input[0])) {
            lexDecimalOrFloat();
        } else if (Character.isUpperCase(input[0])) {
            lexConstructorOrQualifier(null);
        } else if (Character.isLowerCase(input[0]) || input[0] == '_') {
            lexKeywordOrIdentifier();
        } else if (isSymbol(input[0])) {
            lexOperator();
        } else if (is('(') || is(')') || is(',')
                || is(';') || is('[') || is(']')
                || is('`') || is('{') || is('}')) {
            result.add(new Token(
                    Token.Type.punctuation,
                    String.valueOf(input[0])));
            advance(1);
        } else if (is('\'')) {
            lexChar();
        } else if (is('\"')) {
            lexString();
        } else {
            throw new UnexpectedSymbolException(input[0]);
            // TODO: not possible at this point
        }
    }

    private void lexWhitespace() throws IOException {
        // we do not lex whitespaces
        advance(1);
    }

    private void lexNestedComment() throws IOException {
        StringBuilder builder = new StringBuilder();
        int balance = 1;

        // absorb {-
        builder.append(input, 0, 2);
        advance(2);

        do {
            if (is('-', '}')) {
                builder.append(input, 0, 2);
                advance(2);
                if (--balance == 0) {
                    result.add(new Token(Token.Type.comment, builder.toString()));
                }
            } else if (is('{', '-')) {
                builder.append(input, 0, 2);
                advance(2);
                ++balance;
            } else if (isEOF()) {
                result.add(new Token(Token.Type.unidentified, builder.toString()));
                return;
            } else {
                builder.append(input[0]);
                advance(1);
            }
        } while (balance != 0);
    }

    private void lexPragma() throws IOException {
        StringBuilder builder = new StringBuilder();
        int balance = 1;

        // absorb {-#
        builder.append(input, 0, 3);
        advance(3);

        do {
            if (is('#', '-', '}')) {
                builder.append(input, 0, 3);
                advance(3);
                if (--balance == 0) {
                    result.add(new Token(Token.Type.pragma, builder.toString()));
                }
            } else if (is('-', '}')) {
                builder.append(input, 0, 2);
                advance(2);
                if (--balance == 0) {
                    result.add(new Token(Token.Type.comment, builder.toString()));
                }
            } else if (is('{', '-')) {
                builder.append(input, 0, 2);
                advance(2);
                ++balance;
            } else if (isEOF()) {
                result.add(new Token(Token.Type.unidentified, builder.toString()));
                return;
            } else {
                builder.append(input[0]);
                advance(1);
            }
        } while (balance != 0);
    }

    private void lexDashes() throws IOException {
        StringBuilder builder = new StringBuilder();

        // absorb --
        builder.append(input, 0, 2);
        advance(2);

        /*  absorb all symbols in order to decide
            whether the dashes begin a comment
            or an operator
         */
        while (isSymbol(input[0])) {
            if (isEOF()) {
                String absorbed = builder.toString();
                if (isAllDashes(absorbed)) {
                    // normal comment should end with line break
                    result.add(new Token(Token.Type.unidentified, absorbed));
                    return;
                } else {
                    result.add(new Token(Token.Type.operator, absorbed));
                    return;
                }
            } else {
                builder.append(input[0]);
                advance(1);
            }
        }


        if (isAllDashes(builder.toString())) {
            // parse comment
            while (input[0] != '\n') {
                if (isEOF()) {
                    // normal comment should end with line break
                    result.add(new Token(
                            Token.Type.unidentified, builder.toString()));
                    return;
                } else {
                    // absorb everything up to the line terminator
                    builder.append(input[0]);
                    advance(1);
                }
            }
            result.add(new Token(Token.Type.comment, builder.toString()));
        } else {
            // add operator
            result.add(new Token(Token.Type.operator, builder.toString()));
        }

    }

    private void lexOctDigit() throws IOException {
        StringBuilder builder = new StringBuilder();

        // accept 0oN
        builder.append(input, 0, 3);
        advance(3);

        while (isOctDigit(input[0]) && !isEOF()) {
            builder.append(input[0]);
            advance(1);
        }

        result.add(new Token(Token.Type.numericConstant, builder.toString()));
    }

    private void lexHexDigit() throws IOException {
        StringBuilder builder = new StringBuilder();

        // accept 0xN
        builder.append(input, 0, 3);
        advance(3);

        while (isHexDigit(input[0]) && !isEOF()) {
            builder.append(input[0]);
            advance(1);
        }

        result.add(new Token(Token.Type.numericConstant, builder.toString()));
    }

    private void lexDecimalOrFloat() throws IOException, UnexpectedSymbolException {
        StringBuilder builder = new StringBuilder();
        // integer part
        while (isDigit(input[0]) && !isEOF()) {
            builder.append(input[0]);
            advance(1);
        }
        // fractional part
        if (is('.') && curInputLength > 1 && isDigit(input[1])) {
            builder.append(input[0]);
            advance(1);
            while (isDigit(input[0]) && !isEOF()) {
                builder.append(input[0]);
                advance(1);
            }
        }
        // exponent
        if ( (is('e') || is('E')) ) {
            if (curInputLength > 2
                    && (input[1] == '+' || input[1] == '-')
                    && isDigit(input[2])) {
                advance(3);
                builder.append(input,0,3);
            } else if (curInputLength > 1 && isDigit(input[1])) {
                advance(2);
                builder.append(input,0,2);
            } else {
                /* TODO: recover from error(exponent - current char)
                   treat e|E as the start of the next token
                   (missed whitespace)
                   OR
                   synchronization symbol - whitespace
                */
                //throw new UnexpectedSymbolException(input[0]);
                result.add(new Token(Token.Type.unidentified, builder.toString()));
                return;
            }
            while (isDigit(input[0]) && !isEOF()) {
                builder.append(input[0]);
                advance(1);
            }
        }
        result.add(new Token(Token.Type.numericConstant, builder.toString()));
    }

    private void lexConstructorOrQualifier(String qualifier) throws IOException {
        StringBuilder builder = new StringBuilder();
        if (qualifier != null)
            builder.append(qualifier);
        while (!isEOF() && isIdentifier(input[0])) {
            builder.append(input[0]);
            advance(1);
        }

        if (is('.') && curInputLength > 1) {
            builder.append(input[0]);
            advance(1);
            if (Character.isLowerCase(input[0]) || input[0] == '_') {
                // qualified variable identifier
                while (!isEOF() && isIdentifier(input[0])) {
                    builder.append(input[0]);
                    advance(1);
                }
                result.add(new Token(Token.Type.identifier, builder.toString()));
            } else if (Character.isUpperCase(input[0])) {
                // another qualifier or constructor identifier
                lexConstructorOrQualifier(builder.toString());
            } else if (isSymbol(input[0])) {
                // qualified operator
                while (!isEOF() && isSymbol(input[0])) {
                    builder.append(input[0]);
                    advance(1);
                }
                result.add(new Token(Token.Type.operator, builder.toString()));
            }
        } else {
            // constructor identifier
            result.add(new Token(Token.Type.identifier, builder.toString()));
        }
    }

    private void lexKeywordOrIdentifier() throws IOException{
        StringBuilder builder = new StringBuilder();

        while (isIdentifier(input[0]) && !isEOF()) {
            builder.append(input[0]);
            advance(1);
        }

        String word = builder.toString();
        if (reservedIds.contains(word)
                || specialVarIds.contains(word)) {
            result.add(new Token(Token.Type.keyword, word));
        } else {
            result.add(new Token(Token.Type.identifier, word));
        }

    }

    private void lexOperator() throws IOException{
        StringBuilder builder = new StringBuilder();

        while (isSymbol(input[0]) && !isEOF()) {
            builder.append(input[0]);
            advance(1);
        }

        result.add(new Token(Token.Type.operator, builder.toString()));
    }

    private void lexChar() throws IOException, UnexpectedSymbolException {
        StringBuilder builder = new StringBuilder();

        // absorb '
        builder.append(input[0]);
        advance(1);

        // between ' and '
        if (isEOF()) {
            throw new UnexpectedSymbolException("EOF before character constant closed");
        } else if (is('\\')) {
            lexEscape(builder);
        } else {
            builder.append(input[0]);
            advance(1);
        }

        // closing '
        if (is('\'')) {
            builder.append(input[0]);
            advance(1);
            result.add(new Token(Token.Type.symbolicConstant, builder.toString()));
        } else {
            /* TODO: recover from error(missed closing ' : current char)
               result.add(new Token(Token.Type.unidentified, builder.toString()));
               we do not absorb current char and treat it as the start of the next token
               synchronization symbol - current char
            */
            //throw new UnexpectedSymbolException(input[0]);
            result.add(new Token(Token.Type.unidentified, builder.toString()));
        }
    }

    private void lexString() throws IOException, UnexpectedSymbolException {
        StringBuilder builder = new StringBuilder();

        // absorb "
        builder.append(input[0]);
        advance(1);

        for(;;) {
            if (isEOF()) {
                throw new UnexpectedSymbolException("EOF before string constant closed");
            } else if (is('\\','&')) {
                // null symbol
                builder.append(input,0,2);
                advance(2);
            } else if (is('\\')
                    && curInputLength > 1
                    && Character.isWhitespace(input[1])) {
                // gap
                StringBuilder gapTempBuilder = new StringBuilder();
                gapTempBuilder.append(input,0,2);
                advance(2);
                while (Character.isWhitespace(input[0]) && !isEOF()) {
                    advance(1);
                    gapTempBuilder.append(input[0]);
                }
                if (is('\\')) {
                    advance(1);
                } else {
                    /* TODO: recover from error (gap: ")
                       continue to lex new token after "
                       synchronization symbol - "
                     */
                    //throw new UnexpectedSymbolException(input[0]);
                    builder.append(gapTempBuilder);
                    while (!isEOF() && !is('\"')) {
                        if (is('\\','"')) {
                            builder.append(input,0,2);
                            advance(2);
                        } else {
                            builder.append(input[0]);
                            advance(1);
                        }
                    }
                    if(!isEOF()) {
                        builder.append(input[0]);
                        advance(1);
                    }
                    result.add(new Token(Token.Type.unidentified, builder.toString()));
                    return;
                }
            } else if (is('\\') && curInputLength > 1) {
                // escape sequence
                lexEscape(builder);
            } else if (is('\"')) {
                // end of string literal
                builder.append(input[0]);
                advance(1);
                result.add(new Token(
                        Token.Type.symbolicConstant, builder.toString()));
                return;
            } else {
                // a character
                builder.append(input[0]);
                advance(1);
            }

        }
    }

    private void lexEscape(StringBuilder builder) throws IOException, UnexpectedSymbolException {
        // escape sequences
        builder.append(input[0]);
        advance(1);
        if (isEOF()) {
            throw new UnexpectedSymbolException("EOF before character constant closed");
        } else if (is('a') || is('b') || is('f') || is('r') || is('n')
                || is('t') || is('v') || is('\\') || is('\"') || is('\'')) {
            builder.append(input[0]);
            advance(1);
        } else if (is('^') && curInputLength > 1 ) {
            builder.append(input,0,2);
            advance(2);
        } else if (is('N','U','L') || is('S','O','H') || is('S','T','X')
                || is('E','T','X') || is('E','O','T') || is('E','N','Q')
                || is('A','C','K') || is('B','E','L') || is('D','L','E')
                || is('D','C','1') || is('D','C','2') || is('D','C','3')
                || is('D','C','4') || is('N','A','K') || is('S','Y','N')
                || is('E','T','B') || is('C','A','N') || is('S','U','B')
                || is('E','S','C') || is('D','E','L')) {
            builder.append(input,0,3);
            advance(3);
        } else if (is('B','S') || is('H','T') || is('L','F') || is('V','T')
                || is('F','F') || is('C','R') || is('S','0') || is('S','I')
                || is('E','M') || is('F','S') || is('G','S') || is('R','S')
                || is('U','S') || is('S','P')) {
            builder.append(input,0,2);
            advance(2);
        } else if (is('0')
                && curInputLength > 2
                && Character.toLowerCase(input[1]) == 'o'
                && isOctDigit(input[2])) {
            builder.append(input, 0, 3);
            advance(3);
            while (!isEOF() && isOctDigit(input[0])) {
                builder.append(input[0]);
                advance(1);
            }
        } else if (is('0')
                && curInputLength > 2
                && Character.toLowerCase(input[1]) == 'x'
                && isHexDigit(input[2])) {
            builder.append(input, 0, 3);
            advance(3);
            while (!isEOF() && isHexDigit(input[0])) {
                builder.append(input[0]);
                advance(1);
            }
        } else if (isDigit(input[0])) {
            while (!isEOF() && isDigit(input[0])) {
                builder.append(input[0]);
                advance(1);
            }
        } else {
            throw new UnexpectedSymbolException("Illegal escape sequence");
            /* TODO: recover from error (escape seq: " | ')
               catch this exception in lexString and lexChar methods
               continue to lex next token after meeting " or '
               synchronization symbols - " and ' respectively
            */
        }
    }

}
