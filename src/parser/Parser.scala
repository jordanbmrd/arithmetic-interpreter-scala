package parser
import ast.Term
import ast.Op
import lexer.{Lexer, Token}
import lexer.Token.*

object Parser:
  def parse(token: Token): Term =
    token match
      case NUMBER(value) => Term.Number(value)
      case LPAR =>
        val exp = parse(Lexer.nextToken())
        Lexer.nextToken() match
          case RPAR => exp
          case other => throw new Exception(s"Expected RPAR, found: $other")
      case PLUS => Term.BinaryExp(Op.Plus, parse(Lexer.nextToken()), parse(Lexer.nextToken()))
      case MINUS => Term.BinaryExp(Op.Minus, parse(Lexer.nextToken()), parse(Lexer.nextToken()))
      case TIMES => Term.BinaryExp(Op.Times, parse(Lexer.nextToken()), parse(Lexer.nextToken()))
      case DIV => Term.BinaryExp(Op.Div, parse(Lexer.nextToken()), parse(Lexer.nextToken()))
      case IFZ => Term.IfZero(parse(Lexer.nextToken()), parse(Lexer.nextToken()), parse(Lexer.nextToken()))
      case EOF => Term.Number(0)
      case _ => throw new Exception(s"Unexpected token: $token")
