package parser

import ast.Term

import java.io.InputStream

object AbstractParser :
  def analyze(in: InputStream): Term =
    val concreteTree = ConcreteParser.analyze(in)
    val PCFvisitor = new PCFASTVisitor()
    val term = PCFvisitor.visit(concreteTree)
    println(s"AST: $term")
    term