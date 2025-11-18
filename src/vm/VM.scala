package vm

import generator.Ins
import generator.Ins.*
import vm.Value.*

import scala.annotation.tailrec

enum Value:
  case IntVal(n: Int)
  case Closure(code: List[Ins], env: Env)
  case RecClosure(code: List[Ins], env: Env)

type Env = List[Value]
// case class VMState(a: Value, s:List[Value|Env], e: Env, c: List[Ins])

object VM:
  def execute(c: List[Ins]): Value =
    execute(IntVal(0), List(), List(), c)

  @tailrec
  def execute(a: Value, s:List[Value|Env], e: Env, c: List[Ins]): Value = (a, s, e, c) match
    case (_, _, _, List()) => a
    case (_, _, _, Push::c) => execute(a, a::s, e, c)
    case (_, _, _, Ldi(n)::c) => execute(IntVal(n), s, e, c)
    case (IntVal(n), IntVal(m)::s, _, Add::c) => execute(IntVal(m+n), s, e, c)
    case (IntVal(n), IntVal(m)::s, _, Sub::c) => execute(IntVal(m-n), s, e, c)
    case (IntVal(n), IntVal(m)::s, _, Mul::c) => execute(IntVal(m*n), s, e, c)
    case (IntVal(n), IntVal(m)::s, _, Div::c) => execute(IntVal(m/n), s, e, c)
    case (IntVal(0), _, _, Test(i, _)::c) => execute(a, s, e, i:::c)
    case (_, _, _, Test(_, j)::c) => execute(a, s, e, j:::c)
    case (_, _, _, Search(p)::c) => execute(e(p), s, e, c)
    case (_, _, _, Bind::c) => execute(a, s, a::e, c)
    case (_, _, _::eTail, Unbind::c) => execute(a, s, eTail, c)
    case (_, _, _, MkClos(body)::c) => execute(Closure(body, e), s, e, c)
    case (_, _, _, MkRecClos(body)::c) => execute(RecClosure(body, e), s, e, c)
    case (arg, Closure(code, envFun)::sTail, eCur, App::c) =>
      execute(arg, eCur::sTail, arg::envFun, code ::: (Ret :: c))
    case (arg, RecClosure(code, envFun)::sTail, eCur, App::c) =>
      val self = RecClosure(code, envFun)
      execute(arg, eCur::sTail, arg::self::envFun, code ::: (Ret :: c))
    case (a, saved :: sTail, _, Ret::c) =>
      saved match
        case envSaved: Env @unchecked => execute(a, sTail, envSaved, c)
        case _ => throw Exception(s"unexpected stack content on Ret: $saved")
    case state => throw Exception(s"unexpected VM state $state")

@main
def test(): Unit =
  println(VM.execute(List(Ldi(1), Push, Ldi(2), Add, Test(List(Ldi(1)),List(Ldi(2))))))



