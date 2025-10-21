package generator

import ast.Term
import Term.*
import Code.*

object Generator:
  def gen(term: Term): Code = term match
    case Number(n) => Ldi(n)
    case _ => ???