# Arithmetic Scala Interpreter

> This project was completed during a practical lab session for the “Compilation et interprétation” course at IMT Atlantique.

### How it works

Steps :

1. Lexing: scan characters and produce tokens like “(”, “)”, “+”, “-”, “*”, “/”, “ifz”, and NUMBER; ignore spaces and newlines.
2. Parsing: consume tokens to build an AST for either NUMBER, (OP Exp Exp), or (ifz Exp Exp Exp).
3. Evaluation: recursively compute the AST’s value: numbers return themselves, binary nodes apply integer ops to their children, ifz evaluates its condition and picks the then- or else-branch based on whether it equals 0.

### Quick examples

- ```(+ 1 (* 2 2)) → build a BinaryExp tree, then compute 1 + (2*2) = 5.```
- ```(ifz 0 41 99) → condition is 0, so result is 41.```
- ```(ifz (+ 1 1) 1 2) → condition is 2 (non-zero), so result is 2.```
