package generator

enum Code:
  case Add, Sub, Mul, Div, Push
  case Ldi(n: Int)
  case Ifz(z: Code, nz: Code)
  case Seq(seq: List[Code])