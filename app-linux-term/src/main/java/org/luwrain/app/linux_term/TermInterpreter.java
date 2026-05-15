// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.app.linux_term;

import java.util.*;
import org.apache.logging.log4j.*;

import org.luwrain.core.*;

import static java.util.Objects.*;

final class TermInterpreter
{
    static private final Logger log = LogManager.getLogger();

    private final Luwrain luwrain;
    private final TermText text;
    private StringBuilder speaking = new StringBuilder();
    private boolean bell = false;

    TermInterpreter(Luwrain luwrain, TermText text)
    {
	this .luwrain = requireNonNull(luwrain, "luwrain can't be null");
	this.text = text;
    }

    void onCommand(Parser.Output cmd)
    {
	if (cmd instanceof Parser.OutputText t)
	{
	    for(int i = 0;i < t.text.length();i++)
	    {
		final var ch = t.text.charAt(i);
		if (ch == 7)
		    bell = true; else
		if (ch == '\b')
		    text.setCursorPos(text.getHotPointY(), text.getHotPointX() - 1); else
	    this.text.writeChar(ch);
	    }
	    if (!t.text.trim().isEmpty())
	    {
	    if (speaking.length()> 0)
		speaking.append(' ');
	    speaking.append(t.text.trim());
	    }
	    return;
	}
	if (cmd instanceof Parser.AnsiCommand ansi)
	{
	    onAnsiCommand(ansi);
	}
    }

    private void onAnsiCommand(Parser.AnsiCommand cmd)
    {
	switch(cmd.description)
	{
	case "EraseInLine": {
	    final int n = !cmd.params.isEmpty() ? cmd.params.get(0).intValue() : 1;
	    log.trace("Erasing {} chars", n);
	    text.fillSpaces(n);
	    break;
	}
	default:
	    log.warn("Unhandled {}", cmd.toString());
	}
    }

    void speak()
    {
	final boolean playBell = bell;
	final var text = new String(speaking);
	speaking = new StringBuilder();
	bell = false;
	if (text.trim().isEmpty() && !playBell)
	    return;
	if (playBell)
	    		luwrain.playSound(Sounds.TERM_BELL);
	final StringBuilder str = new StringBuilder();
	for(int i = 0;i < text.length();i++)
	{
	    final char ch = text.charAt(i);
	    if (ch < 32)
		str.append(" "); else
	    str.append(ch);
	}
	final String toSpeak = new String(str).trim();
	if (toSpeak.isEmpty())
	    return;
	if (toSpeak.length() == 1)
	    luwrain.speakLetter(toSpeak.charAt(0)); else
	    luwrain.speak(luwrain.getSpeakableText(toSpeak, Luwrain.SpeakableTextType.PROGRAMMING));
    }
    }
