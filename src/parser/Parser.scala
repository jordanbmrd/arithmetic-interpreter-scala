package parser
import ast.Exp
import ast.Op
import lexer.{Lexer, Token}
import lexer.Token.*

object Parser:
  def parse(token: Token): Exp =
    token match
      case NUMBER(value) => Exp.Number(value)
      case LPAR =>
        val exp = parse(Lexer.nextToken())
        Lexer.nextToken() match
          case RPAR => exp
          case other => throw new Exception(s"Expected RPAR, found: $other")
      case PLUS => Exp.BinaryExp(Op.Plus, parse(Lexer.nextToken()), parse(Lexer.nextToken()))
      case MINUS => Exp.BinaryExp(Op.Minus, parse(Lexer.nextToken()), parse(Lexer.nextToken()))
      case TIMES => Exp.BinaryExp(Op.Times, parse(Lexer.nextToken()), parse(Lexer.nextToken()))
      case DIV => Exp.BinaryExp(Op.Div, parse(Lexer.nextToken()), parse(Lexer.nextToken()))
      case IFZ => Exp.IfZero(parse(Lexer.nextToken()), parse(Lexer.nextToken()), parse(Lexer.nextToken()))
      case EOF => Exp.Number(0)
      case _ => throw new Exception(s"Unexpected token: $token")
