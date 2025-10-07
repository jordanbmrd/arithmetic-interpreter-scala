package evaluator

import ast.Term
import ast.Term.*
import ast.Op.*

object Evaluator :
  type Env = Map[String, Int]
  def eval(exp: Term, e: Env): Int = exp match
    case Number(value) => value
    case IfZero(cond, zBranch, nzBranch) =>
      if eval(cond, e) == 0 then eval(zBranch, e) else eval(nzBranch, e)
    case BinaryExp(op, exp1, exp2) =>
      val v1 = eval(exp1, e)
      val v2 = eval(exp2, e)
      op match
        case Plus => v1 + v2
        case Minus => v1 - v2
        case Times => v1 * v2
        case Div => v1 / v2
    case Var(name) =>
      e.getOrElse(name, throw new Exception("Unbound variable: " + name))
    case Let(name, valueExp, body) =>
      val v = eval(valueExp, e)
      eval(body, e + (name -> v))