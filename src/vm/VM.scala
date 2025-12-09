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

object VM:
  def execute(c: List[Ins]): Value =
    execute(List(), List(), c)

  @tailrec
  def execute(s:List[Value|Env], e: Env, c: List[Ins]): Value = (s, e, c) match
    case ((v: Value) :: _, _, List()) => v

    case (s, e, Ldi(n) :: c) => execute(IntVal(n) :: s, e, c)

    case (s, e, PushEnv :: c) => execute(e :: s, e, c)
    case ((v: Value) :: sTail, e, Extend :: c) => execute(sTail, v :: e, c)
    case ((res: Value) :: (envSaved: Env) :: sTail, _, PopEnv :: c) => execute(res :: sTail, envSaved, c)

    case (IntVal(n) :: IntVal(m) :: s, e, Add :: c) => execute(IntVal(m + n) :: s, e, c)
    case (IntVal(n) :: IntVal(m) :: s, e, Sub :: c) => execute(IntVal(m - n) :: s, e, c)
    case (IntVal(n) :: IntVal(m) :: s, e, Mul :: c) => execute(IntVal(m * n) :: s, e, c)
    case (IntVal(n) :: IntVal(m) :: s, e, Div :: c) => execute(IntVal(m / n) :: s, e, c)

    case (IntVal(0) :: s, e, Test(i, _) :: c) => execute(s, e, i ::: c)
    case (IntVal(_) :: s, e, Test(_, j) :: c) => execute(s, e, j ::: c)

    case (s, e, Search(p) :: c) => execute(e(p) :: s, e, c)

    case (s, e, MkClos(_, body) :: c) => execute(Closure(body, e) :: s, e, c)
    case (s, e, MkRecClos(_, body) :: c) => execute(RecClosure(body, e) :: s, e, c)

    case ((arg: Value) :: Closure(code, envFun) :: sTail, eCur, App :: c) =>
      execute(eCur :: sTail, arg :: envFun, code ::: (Ret :: c))
    case ((arg: Value) :: RecClosure(code, envFun) :: sTail, eCur, App :: c) =>
      val self = RecClosure(code, envFun)
      execute(eCur :: sTail, arg :: self :: envFun, code ::: (Ret :: c))

    case ((res: Value) :: (envSaved: Env) :: sTail, _, Ret :: c) =>
      execute(res :: sTail, envSaved, c)

    case state => throw Exception(s"unexpected VM state $state")

@main
def test(): Unit =
  println(VM.execute(List(Ldi(1), Ldi(2), Add, Test(List(Ldi(1)), List(Ldi(2))))))



