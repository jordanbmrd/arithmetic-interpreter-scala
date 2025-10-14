package evaluator

import ast.Term
import ast.Term.*
import ast.Op.*
import evaluator.Value.{Closure, IntVal}

object Evaluator :
  type Env = Map[String, Value]
  given int2Val: Conversion[Int, Value] = n => IntVal(n)
  given val2Int: Conversion[Value, Int] = {
    case IntVal(n) => n
    case Closure(_, _, _) => throw new EvaluationException("Trying to use a function as an integer")
  }

  def eval(exp: Term, e: Env): Value = exp match
    case Number(value) => value
    case IfZero(cond, zBranch, nzBranch) =>
      if eval(cond, e) == IntVal(0) then eval(zBranch, e) else eval(nzBranch, e)
    case BinaryExp(op, exp1, exp2) =>
      val v1 = eval(exp1, e)
      val v2 = eval(exp2, e)
      op match
        case Plus => v1 + v2
        case Minus => v1 - v2
        case Times => v1 * v2
        case Div => v1 / v2
    case Var(name) =>
      e.getOrElse(name, throw new EvaluationException("Variable " + name + " not defined"))
    case Let(name, valueExp, body) =>
      val v = eval(valueExp, e)
      eval(body, e + (name -> v))
    case Fun(param, body) =>
      Closure(param, body, e)
    case App(funExp, argExp) =>
      funExp match
        case Let(name, valueExp, body) =>
          val boundValue = eval(valueExp, e)
          eval(App(body, argExp), e + (name -> boundValue))
        case _ =>
          val funVal = eval(funExp, e)
          val argVal = eval(argExp, e)
          funVal match
            case Closure(param, body, closureEnv) =>
              eval(body, closureEnv + (param -> argVal))
            case _ => throw new EvaluationException("Trying to apply a non-function")