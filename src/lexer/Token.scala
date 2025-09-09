package lexer

enum Token:
  case NUMBER(value: Int)
  case EOF
  case LPAR, RPAR, IFZ
  case PLUS, MINUS, TIMES, DIV