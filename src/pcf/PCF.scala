package pcf

import ast.Term
import evaluator.Evaluator
import generator.{Code, Generator}
import parser.AbstractParser
import typer.Typer

import java.io.{FileInputStream, InputStream}

object PCF:
  def main(args: Array[String]): Unit =
    val in: InputStream =
      if args.isEmpty || args(0).charAt(0) == '-' then
        System.in
      else
        FileInputStream(args(0))

    if (args.contains("-i")) println(s"==> ${interpret(in)}")
    else println(compile(in))

    def interpret(in:InputStream): String =
      val (term, a) = analyze(in)
      val value = Evaluator.eval(term, Map())
      s"$value:$a"

    def analyze(in: InputStream): (Term, typer.Type) =
      val term = AbstractParser.analyze(in)
      val typ = Typer.eval(term, Map())
      (term, typ)

    def compile(in: InputStream): Code =
      val (term, a) = analyze(in)
      val aterm = term.annotate(List()) // calcul des indices de De Bruijn
      println(s"annotated AST: $aterm")
      val code = Generator.gen(term)
      if check(term, code) then code
      else throw Exception("Implementation Error")

    def check(term: Term, code: Code): Boolean =
      val value = Evaluator.eval(term, Map())
      println(value)
      println(code) // in case the execution fails
      val value2 = vm.VM.execute(code)
      value2.toString == value.toString // valid only for PCF green and blue

    // val term = AbstractParser.analyze(in)
    // val typ = Typer.eval(term, Map())
    // println(s"==> ${Evaluator.eval(term, Map())}: $typ")
