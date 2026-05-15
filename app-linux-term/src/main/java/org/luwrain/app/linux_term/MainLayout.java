// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2026 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.app.linux_term;

import java.util.*;
import org.apache.logging.log4j.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.util.*;
import org.luwrain.app.base.*;

final class MainLayout extends LayoutBase
{
    static private final Logger log = LogManager.getLogger();
    
    private final App app;
    private final TermText termText = new TermText(25, 80);
    private final TermInterpreter term ;
    private final NavigationArea termArea;
    private List<String> lines = new ArrayList<>();
    private int oldHotPointX = -1;
    private int oldHotPointY = -1;

    MainLayout(App app)
    {
	this.app = app;
	this.term = new TermInterpreter(app.getLuwrain(), termText);
	this.termArea = new NavigationArea(new DefaultControlContext(app.getLuwrain())){
		@Override public boolean onInputEvent(InputEvent event)
		{
		    if (MainLayout.this.onInputEvent(event))
			return true;
		    		    if (app.onInputEvent(this, event))
			return true;
		    return super.onInputEvent(event);


			    
		    /*
			if (event.getChar() == ' ')
			{
			    final String lastWord = TextUtils.getLastWord(getLine(getHotPointY()), getHotPointX());
			    if (lastWord != null && !lastWord.trim().isEmpty())
				luwrain.speak(lastWord);
			}
			term.write(event.getChar());
			return true;
		    }
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
		    return termText.getLineCount();
		    		}
		@Override public String getLine(int index)
		{
		    return termText.getLine(index);
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

    private boolean onInputEvent(InputEvent event)
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
											    														    app.sendChar(new byte[]{ '\033', '[', 'A' });
																									    			    return true;
																												    			    			    		    			case 			    ALTERNATIVE_ARROW_DOWN:
											    														    app.sendChar(new byte[]{ '\033', '[', 'B' });
																									    			    return true;

			    			    			    		    			case 			    ALTERNATIVE_ARROW_LEFT:
														    app.sendChar(new byte[]{ '\033', '[', 'D' });
			    return true;
			    			    			    			    		    			case 			    ALTERNATIVE_ARROW_RIGHT:
														    app.sendChar(new byte[]{ '\033', '[', 'C' });
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
		    return false;
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

    //Receives new term text for processing
    void termText(String text)
    {
	final var p = new Parser();
	final var cmd = p.parse(text);
	for(var i: cmd)
	{
	    if (i instanceof Parser.AnsiCommand)
	    log.trace(i.toString());
	    term.onCommand(i);
	}
	termArea.setHotPoint(termText.getHotPointX(), termText.getHotPointY());
	term.speak();	
    }

    AreaLayout getLayout()
    {
	return new AreaLayout(termArea);
    }
}
