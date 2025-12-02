package ast

import parser.SyntaxError

enum Term:
  case Number(value: Int)
  case IfZero(cond: Term, zBranch: Term, nzBranch: Term)
  case BinaryExp(op: Op, exp1: Term, exp2: Term)
  case Var(name: String)
  case Let(name: String, value: Term, body: Term)   // let name = value in body
  case Fun(param: String, body: Term)               // fun param -> body
  case App(funExp: Term, argExp: Term)              // funExp argExp
  case Fix(name: String, body: Term)                // fix name body
  // Annotated version: body must be a function; normalized to FixFun
  case FixFun(name: String, param: String, body: Term)

  def annotate(e: List[String]): ATerm = this match
    case n @ Number(_) => n
    case Var(name) => Var(name)
    case BinaryExp(op, u, v) => BinaryExp(op, u.annotate(e), v.annotate(e))
    case IfZero(c, z, nz) => IfZero(c.annotate(e), z.annotate(e), nz.annotate(e))
    case Let(name, value, body) =>
      Let(name, value.annotate(e), body.annotate(name :: e))
    case Fun(param, body) =>
      Fun(param, body.annotate(param :: e))
    case App(f, a) =>
      App(f.annotate(e), a.annotate(e))
    case Fix(name, body) =>
      body match
        case Fun(param, inner) =>
          // In annotated AST, represent as FixFun
          FixFun(name, param, inner.annotate(param :: name :: e))
        case _ =>
          throw new SyntaxError("Syntax error: fix body must be a function")
    case FixFun(name, param, body) =>
      // Already annotated form; ensure children are annotated
      FixFun(name, param, body.annotate(param :: name :: e))

type ATerm = Term

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

