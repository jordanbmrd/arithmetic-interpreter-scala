package test
import pcf.PCF.main

@main
def test(): Unit =
  test("blue1")
  test("blue2")
  test("blue3")
  test("blue4")
  test("blue5")
  test("blue6")
  test("blue7")
  test("blue8")
  test("blue9")

def test(file: String): Unit =
  val args = Array("test/" + file + ".pcf")
  println(s"********** $file")
  try
    main(args)
  catch
    case e: Exception => println(e.getMessage)
