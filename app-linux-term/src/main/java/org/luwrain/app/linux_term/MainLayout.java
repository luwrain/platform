// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.app.linux_term;

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.util.*;
import org.luwrain.app.base.*;

final class MainLayout extends LayoutBase
{
    private final App app;
    private final Terminal term ;
    private final NavigationArea termArea;
    private List<String> lines = new ArrayList<>();
    private int oldHotPointX = -1;
    private int oldHotPointY = -1;

    MainLayout(App app)
    {
	this.app = app;
	this.term = new Terminal(app.getLuwrain(), app.termInfo);
	this.termArea = new NavigationArea(new DefaultControlContext(app.getLuwrain())){
		@Override public boolean onInputEvent(InputEvent event)
		{
		    if (event.isSpecial() && !event.isModified())
			switch(event.getSpecial())
			{
			case ENTER:
			    app.sendChar('\n');
			    return true;
			    			case TAB:
app.sendChar('\t');
			    return true;
			    			case BACKSPACE:
			    app.sendChar('\b');
						    return true;
			case ESCAPE:
			    app.closeApp();
			    return true;
			    			    		    			case 			    ALTERNATIVE_ARROW_UP:
app.sendChar((char)0x1b);
app.sendChar((char)0x5b);
app.sendChar((char)0x41);
			    return true;

			    			    			    		    			case 			    ALTERNATIVE_ARROW_LEFT:
app.sendChar((char)0x1b);
app.sendChar((char)0x5b);
app.sendChar((char)0x44);

			    return true;


			}
		    if (!event.isSpecial() && event.withControlOnly())
			switch(event.getChar())
			{
			case 'd':
			    app.sendChar('\u0004');
			    return true;


			    
			}
		    if (!event.isSpecial() && (!event.isModified() || event.withShiftOnly()))
		    {
			app.sendChar(event.getChar());
			return true;
		    }
		    		    if (app.onInputEvent(this, event))
			return true;
		    return super.onInputEvent(event);


			    
		    /*
			case 			    ALTERNATIVE_ARROW_LEFT:
			    term.writeCode(Terminal.Codes.ARROW_LEFT);
			    return true;
			case 			    ALTERNATIVE_ARROW_RIGHT:
			    term.writeCode(Terminal.Codes.ARROW_RIGHT);
			    return true;
			case 			    ALTERNATIVE_ARROW_DOWN:
			    term.writeCode(Terminal.Codes.ARROW_DOWN);
			    return true;
			}
		    if (!event.isSpecial())
		    {
			if (event.getChar() == ' ')
			{
			    final String lastWord = TextUtils.getLastWord(getLine(getHotPointY()), getHotPointX());
			    if (lastWord != null && !lastWord.trim().isEmpty())
				luwrain.speak(lastWord);
			}
			term.write(event.getChar());
			return true;
		    }
		    //FIXME:
		    return super.onInputEvent(event);
		    */
		}
		@Override public boolean onSystemEvent(SystemEvent event)
		{
		    if (event.getType() == SystemEvent.Type.REGULAR)
			switch(event.getCode())
			{
			case CLOSE:
			    			    app.closeApp();
			    return true;
			}
		    if (app.onSystemEvent(this, event))
			return true;
			return super.onSystemEvent(event);
		}
				@Override public int getLineCount()
		{
		    final int count = term.getLineCount();
		    return count > 0?count:1;
		    		}
		@Override public String getLine(int index)
		{
		    return term.getLine(index);
		}
		@Override public void announceLine(int index, String line)
		{
		    defaultLineAnnouncement(context, index, app.getLuwrain().getSpeakableText(line, Luwrain.SpeakableTextType.PROGRAMMING));
		}
		@Override public String getAreaName()
		{
		    return app.getStrings().areaName();
		}
	    };
    }

    void update(char ch)
    {
		    /*
				 boolean bell)
    {
	if (bell)
	    luwrain.playSound(Sounds.TERM_BELL);
	if (text != null && !text.trim().isEmpty())
	{
	    if (text.length() == 1)
		luwrain.speakLetter(text.charAt(0)); else
		luwrain.speak(text);
	}
	if (hotPointX != oldHotPointX || hotPointY != oldHotPointY)
	{
	    if (text == null || text.isEmpty())
	    {
		final String line = area.getLine(hotPointY);
		if (line != null && hotPointX < line.length())
		    luwrain.speakLetter(line.charAt(hotPointX));
	    }
	    area.setHotPoint(hotPointX, hotPointY);
	    oldHotPointX = hotPointX;
	    oldHotPointY = hotPointY;
	}
	luwrain.onAreaNewContent(area);
	    */
	    }

    void termText(String text)
    {
	term.termText(text.replaceAll("\\[\\?2004[hl]", ""));
	termArea.setHotPoint(term.getHotPointX(), term.getHotPointY());
    }

    AreaLayout getLayout()
    {
	return new AreaLayout(termArea);
    }
}
