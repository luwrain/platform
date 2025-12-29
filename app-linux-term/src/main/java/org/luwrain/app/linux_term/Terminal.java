// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.app.linux_term;

import java.util.*;
import org.apache.logging.log4j.*;

import org.luwrain.core.*;

import static java.util.Objects.*;

final class Terminal implements Lines, HotPoint
{
    static private final char ESCAPE = (char)27;
    static private final int MAX_CMD_LEN = 16;
    static private final Logger log = LogManager.getLogger();

    private final Luwrain luwrain;
    private final TermInfo termInfo;
    private List<String> lines = new ArrayList<>();
    private int hotPointX = 0;
    private int hotPointY = 0;
    private StringBuilder escapeSeq = null;

    Terminal(Luwrain luwrain, TermInfo termInfo)
    {
	this .luwrain = requireNonNull(luwrain, "luwrain can't be null");
	this.termInfo = requireNonNull(termInfo, "termInfo can't be null");
    }

    void termText(String text)
    {
	log.trace("Term text " + text.replaceAll("\u0008", "(BS)"));
	if (escapeSeq != null)
	    log.trace("Escape seq: " + new String(escapeSeq)); else
	    log.trace("No escape seq");
	if (text.isEmpty())
	    return;
	if (lines.isEmpty())
	    lines.add("");
	    for(int i = 0;i < text.length();i++)
	    {
		final char ch = text.charAt(i);
		if (ch == ESCAPE)
		{
		    if (escapeSeq != null)
			onText( "(PREV " + new String(escapeSeq) + ")");
		    escapeSeq = new StringBuilder();
		    escapeSeq.append(ch);
		    continue;
		}
		if (escapeSeq != null && escapeSeq.length() == MAX_CMD_LEN)
		{
		    onText("(LONG " + new String(escapeSeq) + ")" + ch);
		    escapeSeq = null;
		    continue;
		}
		if (escapeSeq == null)
		{
		    onChar(ch);
		    continue;
		}

		escapeSeq.append(ch);
		    final var s = new String(escapeSeq);
		    var m = TermInfo.DCH.matcher(s);
		    if (m.find())
		    {
			onText("[DCH " + m.group(1) + "]");
			escapeSeq = null;
continue;
		    }

		    m = TermInfo.ICH.matcher(s);
		    		    if (m.find())
		    {
			onText("[ICH " + m.group(1) + "]");
			escapeSeq = null;
continue;
		    }

				    
		    final var cmd = termInfo.find(s);
		    if (cmd == null)
			continue;

		    		    escapeSeq = null;
		    onText("[" + cmd + "]");

	    }
	    /*
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
	    */
    }

    void onText(String text)
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

    void appendText(String text)
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
