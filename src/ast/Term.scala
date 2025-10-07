package ast

enum Term:
  case Number(value: Int)
  case IfZero(cond: Term, zBranch: Term, nzBranch: Term)
  case BinaryExp(op: Op, exp1: Term, exp2: Term)
  case Var(name: String)
  case Let(name: String, value: Term, body: Term)   // let name = value in body
  case VarDecl(name: String)

enum Op:
  case Plus, Minus, Times, Div

object Op:
  def parse(s: String): Op =
    s match
      case "+" => Plus
      case "-" => Minus
      case "*" => Times
      case "/" => Div
      case _   => throw new Exception(s"Unknown operator: $s")

