package test
import calc.Calc.main

@main
def test(): Unit =
  test("test/test0.calc")

def test(file: String): Unit =
  val args = Array(file)
  main(args)


