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
    val (code, _) = genAM(term, Nil, 0)
    code

  // Functional threading of closure indices during codegen
  // Returns generated code and next free index
  private def genAM(term: Term, env: List[String], nextIdx: Int): (Code, Int) = term match
    case Number(n) => (List(Ldi(n)), nextIdx)

    case Var(name) =>
      val idx = env.indexOf(name)
      if idx < 0 then throw new Exception(s"Unbound variable in code generation: $name")
      else (List(Search(idx)), nextIdx)

    case Fun(param, body) =>
      // Reserve an index for this closure, then generate its body starting at nextIdx + 1
      val thisIdx = nextIdx
      // Important: in WAT, $apply extends env as <arg, <closure, env>>
      // To keep indices aligned, insert a dummy self slot after the parameter.
      val (c_body, after) = genAM(body, param :: "_self" :: env, nextIdx + 1)
      (List(MkClos(thisIdx, c_body)), after)

    case Term.App(funExp, argExp) =>
      val (c_fun, idx1) = genAM(funExp, env, nextIdx)
      val (c_arg, idx2) = genAM(argExp, env, idx1)
      (c_fun ::: c_arg ::: List(Ins.App), idx2)

    case BinaryExp(op, u, v) =>
      val (c_u, idx1) = genAM(u, env, nextIdx)
      val (c_v, idx2) = genAM(v, env, idx1)
      (c_u ::: c_v ::: List(gen_op(op)), idx2)

    case IfZero(cond, zBranch, nzBranch) =>
      val (c_cond, idx1) = genAM(cond, env, nextIdx)
      val (c_z, idx2) = genAM(zBranch, env, idx1)
      val (c_nz, idx3) = genAM(nzBranch, env, idx2)
      (c_cond ::: List(Test(c_z, c_nz)), idx3)

    case Let(name, value, body) =>
      val (c_value, idx1) = genAM(value, env, nextIdx)
      val (c_body, idx2) = genAM(body, name :: env, idx1)
      (PushEnv :: c_value ::: (Extend :: c_body) ::: List(PopEnv), idx2)

    case FixFun(name, param, inner) =>
      val thisIdx = nextIdx
      val (c_inner, after) = genAM(inner, param :: name :: env, nextIdx + 1)
      (List(MkRecClos(thisIdx, c_inner)), after)
    // Backward compatibility: if an unannotated Fix slips through, accept Fun-only
    case Fix(name, body) =>
      body match
        case Fun(param, inner) =>
          val thisIdx = nextIdx
          val (c_inner, after) = genAM(inner, param :: name :: env, nextIdx + 1)
          (List(MkRecClos(thisIdx, c_inner)), after)
        case _ =>
          throw new Exception("Annotated AST expected: Fix body must be a function")

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

  // Entry point: assemble module with prelude, table, main, and closure functions
  def genWAT(code: Code): String =
    val prelRaw = prelude()
    val prel = stripPreludeTable(prelRaw)
    val bodies = collectBodies(code)
    val existing = 0 // we stripped any prelude table; start our closures at 0
    val header = if prel.nonEmpty then prel else "(module"
    val tableSeg = emitTable(bodies.size, existing, declareTable = true)
    val mainFun = genMain(code, existing)
    val functions = emitFunctions(bodies, existing)
    s"""$header
$tableSeg
$mainFun
$functions
)"""

  // Translate AM code into WAT AST
  private def emit(code: Code, offset: Int): CodeWAT =
    code.flatMap(ins => emitIns(ins, offset))

  // Translate a single instruction to WAT AST (incremental coverage)
  private def emitIns(ins: Ins, offset: Int): CodeWAT = ins match
    case Ldi(n) => List(WAT.Ins(s"i32.const $n"))
    case Add    => List(WAT.Ins("i32.add"))
    case Sub    => List(WAT.Ins("i32.sub"))
    case Mul    => List(WAT.Ins("i32.mul"))
    case Div    => List(WAT.Ins("i32.div_s"))
    case Test(i, j) =>
      // if expects non-zero for then-branch; our semantics is zero => i, non-zero => j
      // so insert eqz to invert truthiness
      List(WAT.Ins("i32.eqz"), WAT.Test(emit(i, offset), emit(j, offset)))
    case Search(p) =>
      List(
        WAT.Ins(s"i32.const $p"),
        WAT.Ins("global.get $ENV"),
        WAT.Ins("call $search")
      )
    case MkClos(idx, _) =>
      List(
        WAT.Ins(s"i32.const ${idx + offset}"),
        WAT.Ins("global.get $ENV"),
        WAT.Ins("call $pair")
      )
    case MkRecClos(idx, _) =>
      List(
        WAT.Ins(s"i32.const ${idx + offset}"),
        WAT.Ins("global.get $ENV"),
        WAT.Ins("call $pair")
      )
    case Ins.App =>
      // Stack before: ... closure arg
      // Reorder to: arg closure for $apply (params: W then C)
      List(
        WAT.Ins("local.set $TMP1"), // TMP1 := arg
        WAT.Ins("local.set $TMP2"), // TMP2 := closure
        // Save current ENV
        WAT.Ins("global.get $ENV"),
        // Call apply(W, C)
        WAT.Ins("local.get $TMP1"), // push arg (W)
        WAT.Ins("local.get $TMP2"), // push closure (C)
        WAT.Ins("call $apply"),     // result on stack; ENV set to extended
        // Restore ENV, preserving result via ACC
        WAT.Ins("global.set $ACC"), // ACC := result ; stack has savedEnv
        WAT.Ins("global.set $ENV"), // restore previous ENV
        WAT.Ins("global.get $ACC")  // push result back
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

  // Remove an existing (table ...) block from the prelude, if present
  private def stripPreludeTable(prel: String): String =
    val start = prel.indexOf("(table")
    if start < 0 then prel
    else
      var depth = 0
      var i = start
      var started = false
      while i < prel.length do
        prel.charAt(i) match
          case '(' =>
            depth += 1
            started = true
          case ')' if started =>
            depth -= 1
            if depth == 0 then
              val before = prel.substring(0, start)
              val after = prel.substring(i + 1)
              return before + after
          case _ =>
        i += 1
      prel // fallback: if not balanced, return original

  // ===== Closure body collection (functional style) =====
  // Collects closure bodies in production order
  def collectBodies(code: Code): List[Code] =
    def walk(c: Code, acc: List[Code]): List[Code] = c match
      case Nil => acc
      case MkClos(_, body) :: tail =>
        // first add this body, then traverse inside to collect nested closures, then continue
        val acc1 = acc :+ body
        val acc2 = walk(body, acc1)
        walk(tail, acc2)
      case MkRecClos(_, body) :: tail =>
        val acc1 = acc :+ body
        val acc2 = walk(body, acc1)
        walk(tail, acc2)
      case Test(i, j) :: tail =>
        val acc1 = walk(i, acc)
        val acc2 = walk(j, acc1)
        walk(tail, acc2)
      case _ :: tail => walk(tail, acc)
    walk(code, Nil)

  // ===== Table and functions emission =====
  private def functionName(i: Int): String = s"$$closure$i"

  // Count how many entries are already present in the prelude's table (heuristic)
  private def countPreludeTableEntries(prel: String): Int =
    val elemIdx = prel.indexOf("(elem")
    if elemIdx < 0 then 0
    else
      val endIdx = prel.indexOf(")", elemIdx) match
        case -1 => prel.length
        case j  => j
      val segment = prel.substring(elemIdx, endIdx)
      segment.count(_ == '$')

  // Emit a table declaration or an element segment extending an existing table
  private def emitTable(size: Int, offset: Int, declareTable: Boolean): String =
    if size == 0 then
      if declareTable then
        s"""  (table funcref
  (elem
  ))"""
      else ""
    else
      val elems = (0 until size).map(i => s"    ${functionName(i)}").mkString("\n")
      if declareTable then
        s"""  (table funcref
  (elem
$elems
  )
)"""
      else
        s"""  (elem (i32.const $offset)
$elems
  )"""

  // Emit closure bodies as functions; each returns i32
  private def emitFunctions(bodies: List[Code], offset: Int): String =
    bodies.zipWithIndex.map { case (body, i) => emitFunction(i, body, offset) }.mkString("\n\n")

  private def emitFunction(idx: Int, body: Code, offset: Int): String =
    s"""  (func ${functionName(idx)} (result i32)
    (local $$TMP1 i32) (local $$TMP2 i32)
${format(2, emit(body, offset))}
    (return)
  )"""

  // Emit main function body
  private def genMain(code: Code, offset: Int): String =
    s"""  (func (export "main") (result i32)
    (local $$TMP1 i32) (local $$TMP2 i32)
${format(2, emit(code, offset))}
    return)"""