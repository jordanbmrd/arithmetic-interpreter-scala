package parserPCF

import ast.*
import ast.Term.*
import parserPCF.{PCFBaseVisitor, PCFParser}

import scala.jdk.CollectionConverters.*

class PCFASTVisitor[AST] extends PCFBaseVisitor[AST] :

  override def visitNumber(ctx: PCFParser.NumberContext): AST =
    Number(ctx.getText.toInt).asInstanceOf[AST]

  override def visitBinaryExp(ctx: PCFParser.BinaryExpContext): AST =
    val s = ctx.OP().getText
    val op = Op.parse(s)
    // ctx.term is a Java list, it is translated in a Scala list
    // (initially, to an instance of Buffer, using a collection
    // converter, as Java lists are mutable)
    val concreteExps = ctx.term.asScala.toList
    val List(exp1, exp2) =
      for (concreteExp <- concreteExps) yield
        visit(concreteExp).asInstanceOf[Term]
    BinaryExp(op, exp1, exp2).asInstanceOf[AST]

  override def visitIfZero(ctx: PCFParser.IfZeroContext): AST =
    val concreteExps = ctx.term.asScala.toList
    val List(exp1, exp2, exp3) =
      for (concreteExp <- concreteExps) yield
        visit(concreteExp).asInstanceOf[Term]
    IfZero(exp1, exp2, exp3).asInstanceOf[AST]

  override def visitVar(ctx: PCFParser.VarContext): AST = {
    val name = ctx.ID().getText
    Var(name).asInstanceOf[AST]
  }

  override def visitParExp(ctx: PCFParser.ParExpContext): AST =
    visit(ctx.term())

  override def visitLet(ctx: PCFParser.LetContext): AST =
    val name = ctx.ID().getText
    val concreteExps = ctx.term().asScala.toList
    val List(exp1, exp2) =
      for (concreteExp <- concreteExps) yield
        visit(concreteExp).asInstanceOf[Term]
    Let(name, exp1, exp2).asInstanceOf[AST]

  override def visitFun(ctx: PCFParser.FunContext): AST =
    val name = ctx.ID().getText
    val body = visit(ctx.term()).asInstanceOf[Term]
    Fun(name, body).asInstanceOf[AST]

  override def visitApp(ctx: PCFParser.AppContext): AST =
    val concreteExps = ctx.term().asScala.toList
    val List(funExp, argExp) =
      for (concreteExp <- concreteExps) yield
        visit(concreteExp).asInstanceOf[Term]
    App(funExp, argExp).asInstanceOf[AST]