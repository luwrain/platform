// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.app.linux_term;

import java.util.*;
import org.apache.logging.log4j.*;

import org.luwrain.core.*;

import static java.util.Objects.*;

final class TermInterpreter implements Lines, HotPoint
{
    static private final Logger log = LogManager.getLogger();

    private final Luwrain luwrain;
    private List<String> lines = new ArrayList<>();
    private int hotPointX = 0, hotPointY = 0;
    private StringBuilder escapeSeq = null;

    TermInterpreter(Luwrain luwrain, TermInfo termInfo)
    {
	this .luwrain = requireNonNull(luwrain, "luwrain can't be null");
    }

    void onCommand(Parser.Output cmd)
    {
	if (lines.isEmpty())
	    lines.add("");
	if (cmd instanceof Parser.OutputText text)
	{
	    onText(text.text);
	    return;
	}
    }

    private void onText(String text)
    {
	for(int i = 0;i < text.length();i++)
	    onChar(text.charAt(i));
    }

    void onChar(char ch)
    {    
		    switch(ch)
		    {
		    case (char)7:
			//Doing nothing, sound will be played through speaking
return;
		    case '\b':
						onBackspace();
return;
		    case '\r':
return;
		    case '\n':
			lines.add("");
			hotPointY++;
			hotPointX = 0;
return;
		    default:
			appendText(String.valueOf(ch));
			return;
		    }
    }

    private void appendText(String text)
    {
				lines.set(lines.size() - 1, lines.get(lines.size() - 1) + text);
				hotPointX += text.length();
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

void onBackspace()
    {
	if (hotPointX == 0)
	    return;
	if (hotPointY >= lines.size())
	{
	    log.warn("Hot point Y is outside of text area");
	    return;
	}
	final String line = lines.get(hotPointY);
	hotPointX = Math.min(hotPointX, line.length());
	final char ch = line.charAt(hotPointX - 1);
	lines.set(hotPointY, line.substring(0, hotPointX -1) + line.substring(hotPointX));
	hotPointX--;
	//	luwrain.setEventResponse(DefaultEventResponse.letter(ch));
    }
}
