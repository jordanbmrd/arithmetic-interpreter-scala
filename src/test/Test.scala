package test
import pcf.PCF.main

@main
def test(): Unit =
  test("black0")
  test("black1")
  test("black2")
  test("black3")

def test(file: String): Unit =
  val args = Array("test/" + file + ".pcf")
  println(s"********** $file")
  try
    main(args)
  catch
    case e: Exception => println(e.getMessage)
