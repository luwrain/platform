/*
   Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

   This file is part of LUWRAIN.

   LUWRAIN is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   LUWRAIN is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.app.man;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import org.apache.logging.log4j.*;

import org.luwrain.core.*;
import org.luwrain.controls.*;
import org.luwrain.linux.*;
import org.luwrain.controls.ConsoleUtils.*;
import org.luwrain.app.base.*;

import static org.luwrain.core.DefaultEventResponse.*;

final class MainLayout extends LayoutBase implements ConsoleArea.ClickHandler<String>, ConsoleArea.InputHandler
{
    static private final Logger log = LogManager.getLogger();

    static private final
	Pattern ENTRY_PATTERN = Pattern.compile("^([a-zA-Z0-9_-]+)\\s+\\(([0-9a-zA-Z_-]+)\\)\\s.*$", Pattern.CASE_INSENSITIVE);

    private final App app;
    private final ConsoleArea<String> searchArea;
    private final SimpleArea pageArea;

    private String[] pages = new String[0];

    MainLayout(App app)
    {
	super(app);
	this.app = app;
	this.searchArea = new ConsoleArea<>(consoleParams((params)->{
		    params.model = new ArrayModel<>(()->pages);
		    params.appearance = new SearchAreaAppearance();
		    params.name = app.getStrings().searchAreaName();
		    params.inputPos = ConsoleArea.InputPos.TOP;
		    params.inputPrefix = "man>";
		    params.inputHandler = this;
		    params.clickHandler = this;
		}));
	this.pageArea = new SimpleArea(getControlContext(), app.getStrings().pageAreaName());
	setAreaLayout(AreaLayout.TOP_BOTTOM, searchArea, null, pageArea, null);
    }

    @Override public boolean onConsoleClick(ConsoleArea area, int index, String item)
    {
	final Matcher m = ENTRY_PATTERN.matcher(item.trim());
	if (!m.find())
	    return false;
	final var output = new BashProcessOutput();
	final var p = new BashProcess("man " + BashProcess.escape(m.group(2)) + " " + BashProcess.escape(m.group(1)), output);
	try {
	    p.run();
	}
	catch(IOException e)
	{
	    app.crash(e);
	    return true;
	}
	final int exitCode = p.waitFor();
	if (exitCode != 0)
	{
	    app.getLuwrain().playSound(Sounds.ERROR);
	    for(String s: output.getErrors())
		log.error("Man: " + s);
	    return true;
	}
	pageArea.setLines(output.getOutputAsArray());
	pageArea.setHotPoint(0, 0);
	setActiveArea(pageArea);
	return true;
    }

    @Override public ConsoleArea.InputHandler.Result onConsoleInput(ConsoleArea area, String text)
    {
	if (text.trim().isEmpty())
	    return ConsoleArea.InputHandler.Result.REJECTED;
	if (!search(text.trim().toLowerCase()))
	    return ConsoleArea.InputHandler.Result.REJECTED;
	area.refresh();
	return ConsoleArea.InputHandler.Result.OK;
    }

    boolean search(String query)
    {
	final var output = new BashProcessOutput();
	final BashProcess p = new BashProcess("man -k " + BashProcess.escape(query.trim()), output);
	try {
	    p.run();
	}
	catch(IOException e)
	{
	    app.getLuwrain().crash(e);
	    return true;
	}
	final int exitCode = p.waitFor();
	if (exitCode != 0)
	{
	    final String[] errors = output.getErrorsAsArray();
	    if (errors.length > 0)
		app.getLuwrain().message(errors[0], Luwrain.MessageType.ERROR); else
		app.getLuwrain().playSound(Sounds.ERROR);
	    return true;
	}
	app.getLuwrain().playSound(Sounds.OK);
	pages = output.getOutputAsArray();
	return true;
    }

    private final class SearchAreaAppearance implements ConsoleArea.Appearance<String>
    {
	@Override public void announceItem(String item) { app.setEventResponse(listItem(app.getLuwrain().getSpeakableText(item, Luwrain.SpeakableTextType.PROGRAMMING), Suggestions.CLICKABLE_LIST_ITEM)); }
	@Override public String getTextAppearance(String item) { return item; }
    }
}
