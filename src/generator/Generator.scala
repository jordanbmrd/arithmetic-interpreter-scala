package generator

import ast.{Op, Term}
import Term.*
import Ins.*
import scala.io.Source

type Code = List[Ins]

object Generator:
  // Two-step generation API
  def gen(aTerm: Term): String =
    genWAT(genAM(aTerm))

  // Abstract machine code generation (previously 'gen')
  def genAM(term: Term): Code =
    genAM(term, Nil)

  private def genAM(term: Term, env: List[String]): Code = term match
    case Number(n) => List(Ldi(n))

    case Var(name) =>
      val idx = env.indexOf(name)
      if idx < 0 then throw new Exception(s"Unbound variable in code generation: $name")
      else List(Search(idx))

    case Fun(param, body) =>
      val c_body = genAM(body, param :: env)
      List(MkClos(c_body))

    case Term.App(funExp, argExp) =>
      val c_fun = genAM(funExp, env)
      val c_arg = genAM(argExp, env)
      c_fun ::: c_arg ::: List(Ins.App)

    case BinaryExp(op, u, v) =>
      val c_u = genAM(u, env)
      val c_v = genAM(v, env)
      c_u ::: c_v ::: List(gen_op(op))

    case IfZero(cond, zBranch, nzBranch) =>
      val c_cond = genAM(cond, env)
      val c_z = genAM(zBranch, env)
      val c_nz = genAM(nzBranch, env)
      c_cond ::: List(Test(c_z, c_nz))

    case Let(name, value, body) =>
      val c_value = genAM(value, env)
      val c_body = genAM(body, name :: env)
      PushEnv :: c_value ::: (Extend :: c_body) ::: List(PopEnv)

    case Fix(name, body) =>
      body match
        case Fun(param, inner) =>
          val c_inner = genAM(inner, param :: name :: env)
          List(MkRecClos(c_inner))
        case other =>
          val c_body = genAM(other, name :: env)
          List(MkRecClos(c_body))

    case _ =>
      throw new Exception(s"Code generation not implemented for term: $term")

    def gen_op(op: Op): Ins = op match
      case Op.Plus  => Add
      case Op.Minus => Sub
      case Op.Times => Mul
      case Op.Div   => Div

  // ===== WAT Generation (Step 2) =====
  // Structured low-level representation to ease formatting
  type CodeWAT = List[WAT]
  enum WAT:
    case Ins(ins: String)
    case Test(code1: CodeWAT, code2: CodeWAT)

  // Entry point: create minimal module skeleton with a main function
  def genWAT(code: Code): String =
    val prel = prelude()
    if prel.nonEmpty then
      s"""${prel}
  (func (export "main") (result i32)
${format(1, emit(code))}
  return))"""
    else
      s"""(module (func (export "main") (result i32)
${format(1, emit(code))}
  return))"""

  // Translate AM code into WAT AST
  private def emit(code: Code): CodeWAT =
    code.flatMap(emitIns)

  // Translate a single instruction to WAT AST (incremental coverage)
  private def emitIns(ins: Ins): CodeWAT = ins match
    case Ldi(n) => List(WAT.Ins(s"i32.const $n"))
    case Add    => List(WAT.Ins("i32.add"))
    case Sub    => List(WAT.Ins("i32.sub"))
    case Mul    => List(WAT.Ins("i32.mul"))
    case Div    => List(WAT.Ins("i32.div_s"))
    case Test(i, j) =>
      // if expects non-zero for then-branch; our semantics is zero => i, non-zero => j
      // so insert eqz to invert truthiness
      List(WAT.Ins("i32.eqz"), WAT.Test(emit(i), emit(j)))
    case Search(p) =>
      List(
        WAT.Ins(s"i32.const $p"),
        WAT.Ins("global.get $ENV"),
        WAT.Ins("call $search")
      )
    case PushEnv =>
      List(WAT.Ins("global.get $ENV"))
    case Extend =>
      List(
        // stack: ... v
        WAT.Ins("global.get $ENV"), // ... v env
        WAT.Ins("call $cons"),      // ... env' = cons(v, env)
        WAT.Ins("global.set $ENV")  // update ENV
      )
    case PopEnv =>
      List(
        // stack: ... v env_saved
        WAT.Ins("global.set $ACC"), // ACC := v ; stack: ... env_saved
        WAT.Ins("global.set $ENV"), // ENV := env_saved ; stack: ...
        WAT.Ins("global.get $ACC")  // push v
      )
    case other =>
      throw new Exception(s"WAT generation not implemented for instruction: $other")

  // Pretty-printing helpers
  private def spaces(depth: Int): String =
    (for _ <- 0 until depth yield "  ").mkString

  private def format(depth: Int, code: CodeWAT): String =
    code.map(formatIns(depth, _)).mkString("\n")

  private def formatIns(depth: Int, ins: WAT): String = ins match
    case WAT.Ins(s) =>
      s"${spaces(depth)}$s"
    case WAT.Test(code1, code2) =>
      val thenPart = format(depth + 2, code1)
      val elsePart = format(depth + 2, code2)
      s"""${spaces(depth)}(if (result i32)
${spaces(depth + 1)}(then
$thenPart
${spaces(depth + 1)})
${spaces(depth + 1)}(else
$elsePart
${spaces(depth + 1)})
${spaces(depth)})"""

  // Prelude handling: include memory/globals and helper functions up to $search
  private def prelude(): String =
    // Try paths in order: explicit prelude, src/test/am.wat, test/am.wat
    val paths = List(
      "wat/prelude.wat",
      "src/test/am.wat",
      "test/am.wat"
    )
    def readIfExists(p: String): Option[String] =
      try
        val src = Source.fromFile(p)
        try Some(src.mkString)
        finally src.close()
      catch case _: Throwable => None
    paths.iterator.flatMap(readIfExists).toSeq.headOption match
      case Some(content) if content.contains("(module") =>
        // If the source contains a full module start, slice before exported main if present
        val idx = content.indexOf("""(func (export "main")""")
        if idx >= 0 then content.substring(0, idx).trim
        else content.trim
      case Some(other) => other.trim
      case None => ""