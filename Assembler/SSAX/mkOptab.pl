#!/usr/bin/perl -w

use strict;


close STDOUT;
open STDOUT, "> Optab.java" or die "Optab.java: can't open for output\n";

print <<'EOF';
import java.util.*;

public class Optab {

    private static class OptabEntry {
	public Token.Val val;
	public int opcode;

	public OptabEntry(Token.Val val, int opcode) {
	    this.val = val;
	    this.opcode = opcode;
	}
    }

    private static Map<String, OptabEntry> optab;

    static {
	optab = new HashMap<String,OptabEntry>();
        // create the entries here
EOF

open OPCODES, "< opcodes" or die "opcodes: no such file\n";    
while (<OPCODES>) {
    next if /^#/;
    chomp;
    my ($op, $name, $arg) = split;
    print "        optab.put(\"$name\", new OptabEntry(Token.Val.";
    if ($op eq "pseudo") {
	print "$name, 0));\n";
	next;
    } else {
	my $m = $arg ? 1 : 0;
	my $tok = "OP$m";
	print "$tok, $op";
    }
    print "));\n";
}

print <<'EOF';
    }

    // check the token t to see if it's an ID or (pseudo)op
    public static void check(Token t) {
	String s = t.str.toString().toUpperCase();
        if (s.equals("SP")) {
            // it's an ID
            t.str = new StringBuffer("SP");
            t.val = Token.Val.ID;
            return;
        }
        // look it up in the op table
        OptabEntry op = optab.get(s);
        if (op == null) {
            // must be an ID
            t.val = Token.Val.ID;
            return;
        }
        t.val = op.val;
        t.opcode = op.opcode;
    }
}
EOF
