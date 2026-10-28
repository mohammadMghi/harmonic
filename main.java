
import java.util.*;
import java.util.regex.*;

public class Main {

    static class Token {
        String type;
        String value;

        Token(String type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return "(" + type + ", " + value + ")";
        }
    }

    static class TokenType {
        String name;
        Pattern pattern;

        TokenType(String name, String regex) {
            this.name = name;
            this.pattern = Pattern.compile(regex);
        }
    }

    static final List<TokenType> TOKEN_TYPES = Arrays.asList(
        new TokenType("KEYWORD", "\\b(let|print|if|else)\\b"),
        new TokenType("IDENTIFIER", "[a-zA-Z_][a-zA-Z0-9_]*"),
        new TokenType("NUMBER", "\\d+"),
        new TokenType("ASSIGN", "="),
        new TokenType("LPAREN", "\\("),
        new TokenType("RPAREN", "\\)"),
        new TokenType("LBRACE", "\\{"),
        new TokenType("RBRACE", "\\}"),
        new TokenType("PLUS", "\\+"),
        new TokenType("MULTIPLY", "\\*"),
        new TokenType("DIVIDE", "/"),
        new TokenType("MINUS", "-"),
        new TokenType("GT", ">"),
        new TokenType("LT", "<"),
        new TokenType("SKIP", "[ \\t\\r]+"),
        new TokenType("NEWLINE", "\\n")
    );

    static List<Token> lexer(String code) {
        List<Token> tokens = new ArrayList<>();
        int pos = 0;

        while (pos < code.length()) {
            boolean matched = false;

            for (TokenType tokenType : TOKEN_TYPES) {
                Matcher matcher = tokenType.pattern.matcher(code);
                matcher.region(pos, code.length());

                if (matcher.lookingAt()) {
                    String value = matcher.group();
                    String kind = tokenType.name;

                    pos = matcher.end();
                    matched = true;

                    if (kind.equals("SKIP")) {
                        break;
                    }

                    if (kind.equals("KEYWORD")) {
                        kind = value.toUpperCase();
                    }

                    tokens.add(new Token(kind, value));
                    break;
                }
            }

            if (!matched) {
                throw new RuntimeException(
                    "Invalid character: " + code.charAt(pos)
                );
            }
        }

        return tokens;
    }

    static List<List<Token>> splitLines(List<Token> tokens) {
        List<List<Token>> lines = new ArrayList<>();
        List<Token> currentLine = new ArrayList<>();

        for (Token token : tokens) {
            if (token.type.equals("NEWLINE")) {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine);
                    currentLine = new ArrayList<>();
                }
            } else {
                currentLine.add(token);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
        }

        return lines;
    }

    static double getValue(
        Token token,
        Map<String, Double> variables
    ) {
        if (token.type.equals("NUMBER")) {
            return Double.parseDouble(token.value);
        }

        if (token.type.equals("IDENTIFIER")) {
            if (!variables.containsKey(token.value)) {
                throw new RuntimeException(
                    "Undefined variable: " + token.value
                );
            }

            return variables.get(token.value);
        }

        throw new RuntimeException(
            "Invalid value: " + token.value
        );
    }

    static void validateExpression(List<Token> tokens) {
        if (tokens.isEmpty()) {
            throw new RuntimeException("Empty expression");
        }

        boolean expectingValue = true;
        int parenCount = 0;

        for (Token token : tokens) {
            String kind = token.type;
            String value = token.value;

            if (expectingValue) {
                if (kind.equals("NUMBER")
                    || kind.equals("IDENTIFIER")) {

                    expectingValue = false;

                } else if (kind.equals("LPAREN")) {
                    parenCount++;

                } else {
                    throw new RuntimeException(
                        "Expected a value, got '" + value + "'"
                    );
                }

            } else {
                if (kind.equals("PLUS")
                    || kind.equals("MINUS")
                    || kind.equals("MULTIPLY")
                    || kind.equals("DIVIDE")) {

                    expectingValue = true;

                } else if (kind.equals("RPAREN")) {
                    parenCount--;

                    if (parenCount < 0) {
                        throw new RuntimeException(
                            "Unexpected closing parenthesis"
                        );
                    }

                } else {
                    throw new RuntimeException(
                        "Expected an operator, got '" + value + "'"
                    );
                }
            }
        }

        if (expectingValue) {
            throw new RuntimeException(
                "Expression cannot end with an operator"
            );
        }

        if (parenCount != 0) {
            throw new RuntimeException(
                "Unclosed parenthesis"
            );
        }
    }

    static double evaluateValues(List<Object> values) {
        List<Object> resultValues = new ArrayList<>(values);

        int i = 1;

        // First: multiplication and division
        while (i < resultValues.size() - 1) {
            String operator = (String) resultValues.get(i);

            double left = (double) resultValues.get(i - 1);
            double right = (double) resultValues.get(i + 1);

            if (operator.equals("MULTIPLY")) {
                resultValues.set(i - 1, left * right);
                resultValues.subList(i, i + 2).clear();

            } else if (operator.equals("DIVIDE")) {
                if (right == 0) {
                    throw new ArithmeticException(
                        "Division by zero"
                    );
                }

                resultValues.set(i - 1, left / right);
                resultValues.subList(i, i + 2).clear();

            } else {
                i += 2;
            }
        }

        // Then: addition and subtraction
        double result = (double) resultValues.get(0);

        for (i = 1; i < resultValues.size(); i += 2) {
            String operator = (String) resultValues.get(i);
            double right = (double) resultValues.get(i + 1);

            if (operator.equals("PLUS")) {
                result += right;

            } else if (operator.equals("MINUS")) {
                result -= right;
            }
        }

        return result;
    }

    static double evaluateValuesWithParentheses(
        List<Object> values
    ) {
        List<Object> resultValues = new ArrayList<>(values);

        while (resultValues.contains("LPAREN")) {
            int j = resultValues.indexOf("LPAREN");
            int k = j + 1;
            int parenCount = 1;

            while (k < resultValues.size() && parenCount > 0) {
                if (resultValues.get(k).equals("LPAREN")) {
                    parenCount++;

                } else if (resultValues.get(k).equals("RPAREN")) {
                    parenCount--;
                }

                k++;
            }

            if (parenCount != 0) {
                throw new RuntimeException(
                    "Unclosed parenthesis"
                );
            }

            List<Object> innerValues = new ArrayList<>(
                resultValues.subList(j + 1, k - 1)
            );

            double innerResult =
                evaluateValuesWithParentheses(innerValues);

            resultValues.subList(j, k).clear();
            resultValues.add(j, innerResult);
        }

        if (resultValues.contains("RPAREN")) {
            throw new RuntimeException(
                "Unexpected closing parenthesis"
            );
        }

        return evaluateValues(resultValues);
    }

    static double evaluateExpression(
        List<Token> tokens,
        Map<String, Double> variables
    ) {
        List<Object> values = new ArrayList<>();

        for (Token token : tokens) {
            if (token.type.equals("NUMBER")
                || token.type.equals("IDENTIFIER")) {

                values.add(getValue(token, variables));

            } else {
                values.add(token.type);
            }
        }

        return evaluateValuesWithParentheses(values);
    }

    static Object parseLet(List<Token> line) {
        if (line.size() < 4) {
            throw new RuntimeException(
                "Invalid let statement"
            );
        }

        if (!line.get(0).type.equals("LET")) {
            throw new RuntimeException("Expected LET");
        }

        if (!line.get(1).type.equals("IDENTIFIER")) {
            throw new RuntimeException("Expected variable name");
        }

        if (!line.get(2).type.equals("ASSIGN")) {
            throw new RuntimeException("Expected '='");
        }

        List<Token> expressionTokens =
            new ArrayList<>(line.subList(3, line.size()));

        validateExpression(expressionTokens);

        return new Object[] {
            line.get(1).value,
            expressionTokens
        };
    }

    static void validateIf(List<Token> tokens) {
        if (tokens.isEmpty()) {
            throw new RuntimeException("Empty expression");
        }

        // TODO: Implement IF condition validation.
    }

    static String formatNumber(double value) {
        if (Double.isFinite(value)
            && value == Math.rint(value)
            && Math.abs(value) < 1e15) {
            return String.format("%.0f", value);
        }

        return Double.toString(value);
    }

    static void parseAndRun(String code) {
        List<Token> tokens = lexer(code);
        List<List<Token>> lines = splitLines(tokens);

        Map<String, Double> variables = new HashMap<>();
        List<Token> ifBlock = new ArrayList<>();

        boolean isNotIf = true;

        for (List<Token> line : lines) {
            if (line.isEmpty()) {
                continue;
            }

            if (line.get(0).type.equals("IF") && isNotIf) {
                isNotIf = false;
                ifBlock.addAll(line);
                continue;
            }

            if (!isNotIf) {
                ifBlock.addAll(line);

                if (line.get(0).type.equals("RBRACE")) {
                    validateIf(ifBlock);
                    isNotIf = true;
                    ifBlock.clear();
                }

                continue;
            }

            if (line.get(0).type.equals("LET")) {
                Object[] parsed = (Object[]) parseLet(line);

                String name = (String) parsed[0];

                @SuppressWarnings("unchecked")
                List<Token> expressionTokens =
                    (List<Token>) parsed[1];

                if (variables.containsKey(name)) {
                    throw new RuntimeException(
                        "Variable already defined: " + name
                    );
                }

                double value = evaluateExpression(
                    expressionTokens,
                    variables
                );

                variables.put(name, value);

            } else if (line.get(0).type.equals("PRINT")) {
                if (line.size() != 2) {
                    throw new RuntimeException(
                        "Invalid print statement"
                    );
                }

                if (!line.get(1).type.equals("IDENTIFIER")) {
                    throw new RuntimeException(
                        "Expected variable name"
                    );
                }

                String name = line.get(1).value;

                if (!variables.containsKey(name)) {
                    throw new RuntimeException(
                        "Undefined variable: " + name
                    );
                }

                System.out.println(
                    formatNumber(variables.get(name))
                );

            } else {
                throw new RuntimeException(
                    "Unknown statement: " + line.get(0).value
                );
            }
        }

        if (!isNotIf) {
            throw new RuntimeException("Unclosed if block");
        }
    }

    public static void main(String[] args) {
        String code = """
            let a = 2 * (3 + 4)
            let b = 2 + 3 * 4
            let c = 2 * (3 + (4 * 5))
            print a
            print b
            print c
            """;

        parseAndRun(code);
    }
}
