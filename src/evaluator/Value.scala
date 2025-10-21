package evaluator

import ast.Term
import evaluator.Evaluator.Env

enum Value:
  case IntVal(value: Int)
  case Closure(name: String, t: Term, e: Env)

final case class IceCube(name: String, t: Term, e: Env)