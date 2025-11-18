package generator

import ast.{Op, Term}
import Term.*
import Ins.*

type Code = List[Ins]

object Generator:
  def gen(term: Term): Code =
    gen(term, Nil)

  private def gen(term: Term, env: List[String]): Code = term match
    case Number(n) => List(Ldi(n))

    case Var(name) =>
      val idx = env.indexOf(name)
      if idx < 0 then throw new Exception(s"Unbound variable in code generation: $name")
      else List(Search(idx))

    case Fun(param, body) =>
      val c_body = gen(body, param :: env)
      List(MkClos(c_body))

    case Term.App(funExp, argExp) =>
      val c_fun = gen(funExp, env)
      val c_arg = gen(argExp, env)
      c_fun ::: (Push :: c_arg) ::: List(Ins.App)

    case BinaryExp(op, u, v) =>
      val c_u = gen(u, env)
      val c_v = gen(v, env)
      c_u ::: (Push :: c_v) ::: List(gen_op(op))

    case IfZero(cond, zBranch, nzBranch) =>
      val c_cond = gen(cond, env)
      val c_z = gen(zBranch, env)
      val c_nz = gen(nzBranch, env)
      c_cond ::: List(Test(c_z, c_nz))

    case Let(name, value, body) =>
      val c_value = gen(value, env)
      val c_body = gen(body, name :: env)
      c_value ::: (Bind :: c_body) ::: List(Unbind)

    case Fix(name, body) =>
      body match
        case Fun(param, inner) =>
          // Recursive function: compile inner body with param at index 0 and name at index 1
          val c_inner = gen(inner, param :: name :: env)
          List(MkRecClos(c_inner))
        case other =>
          // Fallback (rare in tests): treat as recursive term without parameters
          val c_body = gen(other, name :: env)
          List(MkRecClos(c_body))

    case _ =>
      throw new Exception(s"Code generation not implemented for term: $term")

    def gen_op(op: Op): Ins = op match
      case Op.Plus  => Add
      case Op.Minus => Sub
      case Op.Times => Mul
      case Op.Div   => Div