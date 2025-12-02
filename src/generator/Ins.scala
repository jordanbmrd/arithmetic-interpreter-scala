package generator

enum Ins:
  case Add, Sub, Mul, Div
  case Ldi(n: Int)
  case Ifz(z: Ins, nz: Ins)
  case Test(i: List[Ins], j: List[Ins])
  case Search(p: Int)
  case PushEnv
  case Extend
  case PopEnv
  // Refined: associate a stable index to each closure body
  case MkClos(idx: Int, body: List[Ins])
  case MkRecClos(idx: Int, body: List[Ins])
  case App
  case Ret
