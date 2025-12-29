// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.app.linux_term;

import java.util.*;
import java.util.regex.*;

import org.luwrain.linux.*;

import static java.util.stream.Collectors.*;

final class TermInfo
{
static final Pattern
    DCH = Pattern.compile("\u001b\\[([0-9]+)P"),
    ICH = Pattern.compile("\u001b\\[([0-9]+)@");
    
    final Map<String, String> seq = new HashMap<>();

    
    TermInfo()
    {
	final var cmd = new ShellCmd("TERM=linux infocmp", "/");
	final int exitCode = cmd.waitFor();
	if (exitCode != 0)
	    throw new RuntimeException("infocmp exited with exit code " + exitCode);
	parse(cmd.output.stream()
	      .filter(e -> (!e.trim().isEmpty() && e.trim().charAt(0) != '#'))
	      .collect(joining(" ")));
    }

    private void parse(String text)
    {
var b = new StringBuilder();
	for(int i = 0;i < text.length();i++)
	{
	    final var c = text.charAt(i);
	    if (c == ',' && (i == 0 || text.charAt(i - 1) != '\\'))
	    {
		parseItem(new String(b));
		b = new StringBuilder();
		continue;
	    }
	    if (c == ' ' && b.length() == 0)
		continue;
	    b.append(c);
	}
    }

    private void parseItem(String item)
    {
	final int eqPos = item.indexOf("=");
	if (eqPos < 0)
	    return;
	final String
	name = item.substring(0, eqPos),
	value = item.substring(eqPos + 1)
	.replaceAll("\\\\E", String.valueOf((char)27))
	.replaceAll("\\\\,", ",")
	.replaceAll("\\\\r", String.valueOf((char)13))
		.replaceAll("\\\\n", String.valueOf((char)10));
	//For now ignoring sequences with parameters
	if (value.indexOf("%") >= 0)
	    return;
	seq.put(value, name);
    }

    
String find(String s)
    {
	if (s == null || s.isEmpty())
	    throw new IllegalArgumentException("s can't be empty");
	return seq.get(s);
    }
}
