grammar PCF;

term:
   NUMBER                                           # Number
   | ID                                             # Var
   | '(' term ')'                                   # ParExp
   | term OP term                                   # BinaryExp
   | 'ifz' term 'then' term 'else' term             # IfZero
   | 'let' ID '=' term 'in' term                    # Let
   | 'var' ID                                       # VarDecl
   ;

ID: [a-z][a-z0-9]*;
NUMBER: '0' | [1-9][0-9]* ;
OP: '+' | '-' | '*' | '/' ;
WS: ('\n' | '\r' | '\t' | ' ')+ -> skip ;
