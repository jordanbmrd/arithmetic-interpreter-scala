package generator

import ast.{Op, Term}
import Term.*
import Code.*

object Generator:
  def gen(term: Term): Code = term match
    case Number(n) => Ldi(n)
    case BinaryExp(op, e1, e2) =>
      val c1 = gen(e1)
      val c2 = gen(e2)
      val bin = op match
        case Op.Plus => Add
        case Op.Minus => Sub
        case Op.Times => Mul
        case Op.Div => Div
      Seq(List(c1, c2, bin))
    case IfZero(cond, z, nz) =>
      val cc = gen(cond)
      val cz = gen(z)
      val cnz = gen(nz)
      Seq(List(cc, Ifz(cz, cnz)))