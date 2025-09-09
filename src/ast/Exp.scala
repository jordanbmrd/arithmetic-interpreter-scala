package ast

enum Exp:
  case Number(value: Int)
  case IfZero(cond: Exp, zBranch: Exp, nzBranch: Exp)
  case BinaryExp(op: Op, exp1: Exp, exp2: Exp)

enum Op:
  case Plus, Minus, Times, Div

