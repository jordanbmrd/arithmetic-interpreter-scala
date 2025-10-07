package parserPCF

import ast.Term

import java.io.InputStream

object AbstractParser :
  def analyze(in: InputStream): Term =
    val concreteTree = ConcreteParser.analyze(in)
    val visitor = new PCFASTVisitor();
    visitor.visit(concreteTree).asInstanceOf[Term]
