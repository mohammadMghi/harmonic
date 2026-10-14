import re

TOKEN_TYPES = [
    ('KEYWORD', r'\b(let|print)\b'),
    ('IDENTIFIER', r'[a-zA-Z_][a-zA-Z0-9_]*'),
    ('NUMBER', r'\d+'),
    ('ASSIGN', r'='),
    ('PLUS', r'\+'),
    ('MINUS', r'-'),
    ('SKIP', r'[ \t\r]+'),
    ('NEWLINE', r'\n'),
]

tok_regex = '|'.join(
    f'(?P<{name}>{pattern})'
    for name, pattern in TOKEN_TYPES
)

tok_regex = re.compile(tok_regex)


def lexer(code):
    tokens = []
    pos = 0
   
    while pos < len(code):
        mo = tok_regex.match(code, pos)
       
        if mo is None:
            raise SyntaxError(
                f'Invalid character: {code[pos]!r}'
            )
    
        kind = mo.lastgroup
    
        value = mo.group(kind)

        pos = mo.end()

        if kind == 'SKIP':
            continue

        if kind == 'KEYWORD':
            kind = value.upper()

        tokens.append((kind, value))

    return tokens

def split_lines(tokens):
    lines = []
    current_line = []

    for token in tokens:
        if token[0] == 'NEWLINE':
            if current_line:
                lines.append(current_line)
                current_line = []
        else:
            current_line.append(token)

    if current_line:
        lines.append(current_line)

    return lines

def get_value(token, variables):
    if token[0] == 'NUMBER':
        return int(token[1])

    elif token[0] == 'IDENTIFIER':
        return variables[token[1]]


def evaluate_expression(tokens, variables):
    result = get_value(tokens[0], variables)

    for i in range(1, len(tokens), 2):
        operator = tokens[i][0]
        right = get_value(tokens[i + 1], variables)

        if operator == 'PLUS':
            result += right

        elif operator == 'MINUS':
            result -= right

    return result


def parse_and_run(code):
    tokens = lexer(code)
    lines = split_lines(tokens)

    variables = {}
    
    for line in lines:
        if line[0][0] == 'LET':
            name = line[1][1]
            if name in variables:
                raise NameError(f"Variable already defined: {name}")

            expression_tokens = line[3:]
            value = evaluate_expression(expression_tokens, variables)

            variables[name] = value


        elif line[0][0] == 'PRINT':
            name = line[1][1]

            if name not in variables:
                raise NameError(f"Undefined variable: {name}")

            print(variables[name])
        

code = 'let a = 10\nlet b = a + 5\nprint b'
parse_and_run(code)