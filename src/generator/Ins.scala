package generator

enum Ins:
  case Add, Sub, Mul, Div, Push
  case Ldi(n: Int)
  case Ifz(z: Ins, nz: Ins)
  case Test(i: List[Ins], j: List[Ins])
  case Search(p: Int)
