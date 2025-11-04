package typer

import ast.Term
import ast.Term.*
import unify.TVar

object Typer:
  type Env = Map[String, Type]

  def eval(t: Term, e: Env): Type = t match
    case Number(_) => INT

    case Var(name) =>
      e.getOrElse(name, throw new Exception(s"Variable $name not defined in type environment"))

    case BinaryExp(op, exp1, exp2) =>
      val t1 = eval(exp1, e)
      val t2 = eval(exp2, e)

      if !(t1 === INT) then
        throw new Exception(s"Binary operation expects INT as first argument, got $t1")
      if !(t2 === INT) then
        throw new Exception(s"Binary operation expects INT as second argument, got $t2")
      INT

    case IfZero(cond, zBranch, nzBranch) =>
      val tCond = eval(cond, e)

      if !(tCond === INT) then
        throw new Exception(s"IfZero condition expects INT, got $tCond")

      val tZero = eval(zBranch, e)
      val tNonZero = eval(nzBranch, e)
      if !(tZero === tNonZero) then
        throw new Exception(s"IfZero branches must have the same type: $tZero vs $tNonZero")
      tZero

    case Let(name, value, body) =>
      val tValue = eval(value, e)
      eval(body, e + (name -> tValue))

    case Fun(param, body) =>
      val paramType = TVar()
      val bodyType = eval(body, e + (param -> paramType))
      FUNCTION(paramType, bodyType)

    case App(funExp, argExp) =>
      val funType = eval(funExp, e)
      val argType = eval(argExp, e)
      val resultType = TVar()

      if !(funType === FUNCTION(argType, resultType)) then
        throw new Exception(s"Type mismatch in application: expected function type, got $funType")
      resultType

    case Fix(name, body) =>
      val fixType = TVar()
      val bodyType = eval(body, e + (name -> fixType))
      if !(fixType === bodyType) then
        throw new Exception(s"Fix point type mismatch: $fixType vs $bodyType")
      fixType

