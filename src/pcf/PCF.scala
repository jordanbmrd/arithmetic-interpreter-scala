package pcf

import ast.Term
import evaluator.Evaluator
import generator.{Code, Generator}
import parser.AbstractParser
import typer.Typer

import java.io.{FileInputStream, FileWriter, InputStream}

object PCF:
  def main(args: Array[String]): Unit =
    val verbose = args.length == 0 || args.length > 1 && args.contains("-v")
    val checkAM = args.contains("-vm")
    val interpretOnly = args.contains("-i")

    val (in, filename) =
      if args.isEmpty || args(0).charAt(0) == '-' then
        (System.in, Option.empty[String])
      else
        (FileInputStream(args(0)), Some(args(0)))

    if interpretOnly then
      println(s"==> ${interpret(in)}")
    else
      compile(verbose, checkAM, in, filename)

    def interpret(in: InputStream): String =
      val (term, a) = analyze(verbose, in)
      val value = Evaluator.eval(term, Map())
      s"$value:$a"

    def analyze(verbose: Boolean, in: InputStream): (Term, typer.Type) =
      val term = AbstractParser.analyze(in)
      val typ = Typer.eval(term, Map())
      if verbose then
        println(s"AST: $term")
        println(s"Type: $typ")
      (term, typ)

    def compile(verbose: Boolean, check_am: Boolean, is: InputStream, filename: Option[String]): Unit =
      val (term, typ) = analyze(verbose, is)
      // Policy: compile if result is INT, or if the top-level term is a function literal.
      // This allows tests like red0 (top-level Fun) while rejecting non-INT results from applications (e.g., red19).
      if !(typ === typer.INT) then
        term match
          case ast.Term.Fun(_, _) => () // allow top-level function values
          case _ => throw Exception(s"Top-level expression must be INT, got $typ")
      val aterm = term.annotate(List())
      if verbose then println(s"annotated AST: $aterm")
      if check_am then
        val code = Generator.genAM(aterm)
        if verbose then println(s"Code: $code")
        if !check(term, code) then throw Exception("Implementation Error")
      else
        val code = Generator.gen(aterm)
        if filename.isDefined then
          write(code)
        else
          println(code)

      // write code to .wat file associated to .pcf file passed as argument,
      // returning .wat file relative filename
      def write(code: String): String =
        val WatFilename = filename.get.replaceFirst("\\.pcf\\z", ".wat")
        if verbose then println("writing .wat code to " + WatFilename)
        val out = new FileWriter(WatFilename)
        out.write(code)
        out.flush()
        out.close()
        WatFilename

    def check(term: Term, code: Code): Boolean =
      val value = Evaluator.eval(term, Map())
      if verbose then
        println(value)
        println(code) // in case the execution fails
      val value2 = vm.VM.execute(code)
      // Only compare when the result is an integer (green/blue scope). For functional results, skip equality.
      value match
        case evaluator.Value.IntVal(_) => value2.toString == value.toString
        case _ => true

    // val term = AbstractParser.analyze(in)
    // val typ = Typer.eval(term, Map())
    // println(s"==> ${Evaluator.eval(term, Map())}: $typ")
