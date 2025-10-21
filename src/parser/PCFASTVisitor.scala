package parser

import ast.*
import ast.Term.*
import parser.{PCFBaseVisitor, PCFParser}

import scala.jdk.CollectionConverters.*

class PCFASTVisitor extends PCFBaseVisitor[Term] :
  override def visitNumber(ctx: PCFParser.NumberContext): Term =
    Number(ctx.getText.toInt)

  override def visitBinaryExp1(ctx: PCFParser.BinaryExp1Context): Term =
    val op = Op.parse(ctx.OP1().getText)
    val List(exp1, exp2) = ctx.term().asScala.toList.map(visit)
    BinaryExp(op, exp1, exp2)

  override def visitBinaryExp2(ctx: PCFParser.BinaryExp2Context): Term =
    val op = Op.parse(ctx.OP2().getText)
    val List(exp1, exp2) = ctx.term().asScala.toList.map(visit)
    BinaryExp(op, exp1, exp2)

  override def visitIfZero(ctx: PCFParser.IfZeroContext): Term =
    val List(cond, zBranch, nzBranch) = ctx.term().asScala.toList.map(visit)
    IfZero(cond, zBranch, nzBranch)

  override def visitVar(ctx: PCFParser.VarContext): Term =
    Var(ctx.ID().getText)

  override def visitParExp(ctx: PCFParser.ParExpContext): Term =
    visit(ctx.term())

  override def visitLet(ctx: PCFParser.LetContext): Term =
    val name = ctx.ID().getText
    val List(value, body) = ctx.term().asScala.toList.map(visit)
    Let(name, value, body)

  override def visitFun(ctx: PCFParser.FunContext): Term =
    Fun(ctx.ID().getText, visit(ctx.term()))

  override def visitApp(ctx: PCFParser.AppContext): Term =
    val List(funExp, argExp) = ctx.term().asScala.toList.map(visit)
    App(funExp, argExp)

  override def visitFix(ctx: PCFParser.FixContext): Term =
    Fix(ctx.ID().getText, visit(ctx.term()))

