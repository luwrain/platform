// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.app.linux_term;

import java.util.*;
import org.apache.logging.log4j.*;

import org.luwrain.core.*;
//import org.luwrain.linux.*;

import static java.util.Objects.*;

final class Terminal implements Lines, HotPoint
{
    static private final Logger log = LogManager.getLogger();

    private final Luwrain luwrain;
    private final TermInfo termInfo;
    private List<String> lines = new ArrayList<>();
    private int hotPointX = 0;
    private int hotPointY = 0;
    private StringBuilder seq = new StringBuilder();

    Terminal(Luwrain luwrain, TermInfo termInfo)
    {
	this .luwrain = requireNonNull(luwrain, "luwrain can't be null");
	this.termInfo = requireNonNull(termInfo, "termInfo can't be null");
    }

    void termText(String text)
    {
	if (text.isEmpty())
	    return;
	if (lines.isEmpty())
	    lines.add("");
	final StringBuilder speaking = new StringBuilder();
	try {
	    for(int i = 0;i < text.length();i++)
	    {
		final char ch = text.charAt(i);
		this.seq.append(ch);
		final String seqStr = new String(this.seq);
		final String res = termInfo.find(seqStr);
				//if seq contains the beginning of a some known command, we have to try more characters
		if (res != null && res.isEmpty())
		    continue;
		    this.seq = new StringBuilder();
		if (res == null)
		{
		    speaking.append(seqStr);
		    switch(seqStr)
		    {
		    case "\u0007":
			//Doing nothing, sound will be played through speaking
			continue;
		    case "\b":
			backspace();
			continue;
		    case "\r":
			continue;
		    case "\n":
			lines.add("");
			hotPointY++;
			hotPointX = 0;
			continue;
		    default:
			lines.set(lines.size() - 1, lines.get(lines.size() - 1) + seqStr);
			hotPointX += seqStr.length();
			continue;
		    }
		}
		switch(res)
		{
		case "color":
		case "el"://unknown sequence on backspaces
		    continue;
		    		    case "cr":
			lines.add("");
			hotPointY++;
			hotPointX = 0;
			continue;
		default:
		    log.warn("Unknown terminal command: '" + res + "'");
		    continue;
		}
	    }
	}
	finally {
	    speak(new String(speaking));
	}
    }

    private void speak(String text)
    {
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

    @Override public int getHotPointX()
    {
	return this.hotPointX;
    }

    @Override public int getHotPointY()
    {
	return this.hotPointY;
    }

        @Override public int getLineCount()
    {
	return lines.size();
    }

    @Override public String getLine(int index)
    {
	if (index >= lines.size())
	    return "";
	return lines.get(index);
    }

    private void backspace()
    {
	if (hotPointY >= lines.size())
	    return;
	final String line = lines.get(hotPointY);
	if (hotPointX == 0 || hotPointX > line.length())
	    return;
	final char ch = line.charAt(hotPointX - 1);
	lines.set(hotPointY, line.substring(0, hotPointX -1) + line.substring(hotPointX));
	hotPointX--;
	luwrain.setEventResponse(DefaultEventResponse.letter(ch));
    }
}
