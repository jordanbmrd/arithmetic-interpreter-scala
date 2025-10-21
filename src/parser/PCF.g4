grammar PCF;

term:
   term term                                        # App
   | NUMBER                                         # Number
   | ID                                             # Var
   | '(' term ')'                                   # ParExp
   | term OP1 term                                  # BinaryExp1
   | term OP2 term                                  # BinaryExp2
   | 'ifz' term 'then' term 'else' term             # IfZero
   | 'let' ID '=' term 'in' term                    # Let
   | 'fun' ID '->' term                             # Fun
   | 'fix' ID term                                  # Fix
   ;

ID: [a-z][a-z0-9]*;
NUMBER: '0' | [1-9][0-9]* ;
OP2: '+' | '-' ;
OP1: '*' | '/' ;
WS: ('\n' | '\r' | '\t' | ' ')+ -> skip ;
