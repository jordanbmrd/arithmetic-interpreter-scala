package parserANTLR

import ast.*
import ast.Term.*

import scala.jdk.CollectionConverters.*

class ASTVisitor[AST] extends CalcBaseVisitor[AST] :

  override def visitNumber(ctx: CalcParser.NumberContext): AST =
    Number(ctx.getText.toInt).asInstanceOf[AST]

  override def visitBinaryExp(ctx: CalcParser.BinaryExpContext): AST =
    val s = ctx.OP().getText
    val op = Op.parse(s)
    // ctx.term is a Java list, it is translated in a Scala list
    // (initially, to an instance of Buffer, using a collection
    // converter, as Java lists are mutable)
    val concreteExps = ctx.exp.asScala.toList
    val List(exp1, exp2) =
      for (concreteExp <- concreteExps) yield
        visit(concreteExp).asInstanceOf[Term]
    BinaryExp(op, exp1, exp2).asInstanceOf[AST]

  override def visitIfZero(ctx: CalcParser.IfZeroContext): AST =
    val concreteExps = ctx.exp.asScala.toList
    val List(exp1, exp2, exp3) =
      for (concreteExp <- concreteExps) yield
        visit(concreteExp).asInstanceOf[Term]
    IfZero(exp1, exp2, exp3).asInstanceOf[AST]
