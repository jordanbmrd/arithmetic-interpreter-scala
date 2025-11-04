package generator

import ast.{Op, Term}
import Term.*
import Ins.*

type Code = List[Ins]

object Generator:
  def gen(term: Term): Code = term match
    case Number(n) => List(Ldi(n))
    case BinaryExp(op, u, v) =>
      val c_u = gen(u)
      val c_v = gen(v)
      c_u ::: (Push :: c_v) ::: List(gen_op(op))
    case IfZero(cond, zBranch, nzBranch) =>
      val c_cond = gen(cond)
      val c_z = gen(zBranch)
      val c_nz = gen(nzBranch)
      c_cond ::: List(Test(c_z, c_nz))
    case _ => throw new Exception(s"Code generation not implemented for term: $term")

    def gen_op(op: Op): Ins = op match
      case Op.Plus  => Add
      case Op.Minus => Sub
      case Op.Times => Mul
      case Op.Div   => Div