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

    TermInterpreter(Luwrain luwrain, TermText text)
    {
	this .luwrain = requireNonNull(luwrain, "luwrain can't be null");
	this.text = text;
    }

    void onCommand(Parser.Output cmd)
    {
	if (cmd instanceof Parser.OutputText text)
	{
	    this.text.writeString(text.text);
	    if (!text.text.trim().isEmpty())
	    {
	    if (speaking.length()> 0)
		speaking.append(' ');
	    speaking.append(text.text.trim());
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
	    break;
	}
	default:
	    log.warn("Unhandled {}", cmd.toString());
	}
    }

    void speak()
    {
	final var text = new String(speaking);
	speaking = new StringBuilder();
	if (text.trim().isEmpty())
	    return;
	final StringBuilder str = new StringBuilder();
	for(int i = 0;i < text.length();i++)
	{
	    final char ch = text.charAt(i);
	    if (ch == 7)
	    {
		luwrain.playSound(Sounds.TERM_BELL);
		continue;
	    }
	    if (ch < 32)
	    {
		str.append(" ");
		continue;
	    }
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
