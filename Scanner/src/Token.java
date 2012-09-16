import java.util.*;

public class Token {

    public enum Val {
EOF,		// end of file
AND,		// reserved word
ARRAY,		// reserved word
BEGIN,		// reserved word
CASE,		// reserved word
CONST,		// reserved word
DIV,		// reserved word
DO,		// reserved word
DOWNTO,		// reserved word
ELSE,		// reserved word
END,		// reserved word
FILE,		// reserved word
FOR,		// reserved word
FUNCTION,	// reserved word
GOTO,		// reserved word
IF,		// reserved word
IN,		// reserved word
LABEL,		// reserved word
MOD,		// reserved word
NIL,		// reserved word
NOT,		// reserved word
OF,		// reserved word
OR,		// reserved word
OTHERWISE,	// reserved word
PACKED,		// reserved word
PROCEDURE,	// reserved word
PROGRAM,	// reserved word
RECORD,		// reserved word
REPEAT,		// reserved word
SET,		// reserved word
THEN,		// reserved word
TO,		// reserved word
TYPE,		// reserved word
UNTIL,		// reserved word
VAR,		// reserved word
WHILE,		// reserved word
WITH,		// reserved word
PLUS,		// +
MINUS,		// -
TIMES,		// *
DIVIDE,		// /
LT,		// <
GT,		// >
EQU,		// =
NE,		// <>
LE,		// <=
GE,		// >=
LPAREN,		// (
RPAREN,		// )
LBRACK,		// [
RBRACK,		// ]
DOT,		// .
DOTDOT,		// ..
COMMA,		// ,
SEMI,		// ;
COLON,		// :
UPARROW,	// ^
ASSIGN,		// :=
ID,		// identifier
INT,		// unsigned integer
REAL,		// real number
STRING,		// quoted string
ESTRING,	// error string
COMMENT,	// comment
ECOMMENT,	// error comment
ERROR		// any other character
	}

    public Val val;          // the value of this token
    public StringBuffer str; // the String contents of this token
    public int lno;          // the line number where this token starts

    public Token() {
	val = Val.ERROR;     // assume the worst
	str = new StringBuffer();
	lno = 0;
    }

    public Token (Val val, StringBuffer str, int lno) {
	this.val = val;
	this.str = str;
	this.lno = lno;
    }
}
