package generator

enum Code:
  case Add, Sub, Mul, Div, Push
  case Ldi(n: Int)
  case Seq(seq: List[Code])