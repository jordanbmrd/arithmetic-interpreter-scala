package parser

import ast.Term
import lexer.Lexer

import java.io.InputStream

object AbstractParser:
  def analyze(in: InputStream): Term =
    Lexer(in)
    val exp = Parser.parse(Lexer.nextToken())
    val token = Lexer.nextToken()
    if token != lexer.Token.EOF then
      throw new Exception(s"Unexpected token $token after parsing complete expression $exp")
    else exp