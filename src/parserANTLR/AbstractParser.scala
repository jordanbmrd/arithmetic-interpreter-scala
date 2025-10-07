package parserANTLR

import ast.Term

import java.io.InputStream

object AbstractParser :
  def analyze(in: InputStream): Term =
    val concreteTree = ConcreteParser.analyze(in)
    val visitor = new ASTVisitor
    visitor.visit(concreteTree).asInstanceOf[Term]

