// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import java.io.*;
import java.util.*;

import org.luwrain.core.*;

final class TermInfo
{
final String text;
    private String termName = null;
    private Set<String> values = new HashSet<>();
    private Map<Character, Map<String, String> > seqs = new HashMap<>();

    TermInfo() throws IOException
    {
	final Process p = new ProcessBuilder("infocmp", "linux").start();
	p.getOutputStream().close();
		    	final BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
				final StringBuilder b = new StringBuilder();
	try {
	String line = r.readLine();
	while(line != null)
	{
	    if (!line.isEmpty() && !line.startsWith("#"))
	    b.append(line);
	    line = r.readLine();
	}
	}
	finally {
	    r.close();
	}
	try {
	p.waitFor();
	}
		catch(InterruptedException e)
	{
	    Thread.currentThread().interrupt();
	}
	if (p.exitValue() != 0)
	    throw new IOException("Unable to read the terminfo database, exit value is " + String.valueOf(p.exitValue()));
	this.text = new String(b);
    }

    void read()
    {
	StringBuilder b = new StringBuilder();
	try {
	final StringReader r = new StringReader(text);
	try {
	    //This builder being created indicates that we are in the char code reading mode
	    StringBuilder codeBuilder = null;
	    for(int n = r.read();n >= 0;n = r.read())
	    {
		final char c = (char)n;
		if (codeBuilder != null)
		{
		    if (c >= '0' && c <= '9')
		    {
			codeBuilder.append(c);
			continue;
		    }
		    //The char code sequence ends here, constructing the char and continuing as usual
		    b.append(buildChar(new String(codeBuilder)));
		    codeBuilder = null;
		}
		switch(c)
		{
		case ' ':
		    if (b.length() > 0)
			b.append(' ');
		    continue;
		case '\t':
		    continue;
		case ',':
		    processItem(new String(b));
		    b = new StringBuilder();
		    continue;
		case '\\':
		    {
			int nn = r.read();
			if (nn < 0)
			{
			    b.append('\\');
			    return;
			}
			final char cc = (char)nn;
			switch(cc)
			{
			case 'e':
			case 'E':
			    b.append((char)27);
			    continue;
			case 'n':
			case 'N':
			    b.append('\n');
			    continue;
			case 'r':
			case 'R':
			    b.append('\r');
			    continue;
			case 't':
			case 'T':
			    b.append('\r');
			    continue;
			}
			if (cc < '0' || cc > '9')
			{
			    b.append(cc);
			    continue;
			}
			//Activating the mode of reading the char code
			codeBuilder = new StringBuilder();
			codeBuilder.append(cc);
			continue;
		    }
		default:
		    b.append(c);
		}
	    }
	}
	finally {
	    r.close();
	    if (b.length() > 0)
		processItem(new String(b));
	}
	}
	catch(IOException e)
	{
	    throw new RuntimeException(e);
	}
    }

    private String buildChar(String code)
    {
	try {
	    return Character.toString((char)Integer.parseInt(code));
	}
	catch(NumberFormatException e)
	{
	    return "";
	}
    }

    private void processItem(String text)
    {
	final int pos = text.indexOf("=");
	if (pos > 0 && pos < text.length() - 1)
	{
	    final String name = text.substring(0, pos);
	    final String value = text.substring(pos + 1);
	    final Character c = new Character(value.charAt(0));
	    Map<String, String> m = seqs.get(c);
	    if (m == null)
	    {
		m = new HashMap<>();
	    seqs.put(c, m);
	    }
	    m.put(value, name);
	    return;
	}
	if (termName == null)
	{
	    termName = text;
	    return;
	}
	values.add(text);
    }

    public String find(String seq)
    {
	if (seq.length() == 1 && seq.charAt(0) == 27)
	    return "";//Needs more characters
	//Manually stripping color codes
	if (seq.length() >= 2 && seq.charAt(0) == 27 && seq.charAt(1) == '[')
	{
	    if (seq.length() == 2)
		return "";
	    if (seq.substring(2).matches("^[0-9;]+$"))
		return "";
	    if (seq.endsWith("m"))
		return "color";
	}
	final Map<String, String> m = seqs.get(new Character(seq.charAt(0)));
	if (m == null)
	    return null;
	final String s = m.get(seq);
	if (s != null)
	    return s;
	for(Map.Entry<String, String> e: m.entrySet())
	    if (e.getKey().startsWith(seq))
		return "";
	return null;
    }

    String getTermName()
    {
	return termName != null?termName:"";
    }
}
