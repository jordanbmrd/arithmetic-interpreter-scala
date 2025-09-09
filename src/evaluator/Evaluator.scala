package evaluator

import ast.Exp
import ast.Exp.*
import ast.Op.*

object Evaluator :
  def eval(exp: Exp): Int = exp match
    case Number(value) => value
    case IfZero(cond, zBranch, nzBranch) =>
      if eval(cond) == 0 then eval(zBranch) else eval(nzBranch)
    case BinaryExp(op, exp1, exp2) =>
      op match
        case Plus => eval(exp1) + eval(exp2)
        case Minus => eval(exp1) - eval(exp2)
        case Times => eval(exp1) * eval(exp2)
        case Div => eval(exp1) / eval(exp2)