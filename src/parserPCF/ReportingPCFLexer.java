package parserPCF;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.LexerNoViableAltException;
import parserANTLR.CalcLexer;
import parserANTLR.ErrorFlag;

public class ReportingPCFLexer extends PCFLexer {
    public ReportingPCFLexer(CharStream input) {
        super(input);
    }
    public void recover(LexerNoViableAltException e) {
        ErrorFlag.setFlag(); // report error
        super.recover(e);
    }
}
