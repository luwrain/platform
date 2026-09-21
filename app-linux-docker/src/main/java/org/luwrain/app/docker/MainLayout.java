// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2026 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.app.docker;

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.controls.console.*;
import org.luwrain.popups.*;

import org.luwrain.app.base.*;

import static org.luwrain.core.DefaultEventResponse.*;

final class MainLayout extends LayoutBase
{
    private final App app;
    final List<String> containers = new ArrayList<>();
    final List<String> images = new ArrayList<>();
    final ConsoleArea<String> containersArea;
    final ConsoleArea<String> imagesArea;
    final SimpleArea detailsArea;

    MainLayout(App app)
    {
	super(app);
	this.app = app;
	final var s = app.getStrings();

	containersArea = new ConsoleArea<String>(consoleParams(p -> {
		    p.model = new ListModel<>(containers);
		    p.name = s.containersAreaName();
		    p.appearance = new DefaultAppearance();
		}));

	imagesArea = new ConsoleArea<String>(consoleParams(p -> {
		    p.model = new ListModel<>(images);
		    p.name = s.imagesAreaName();
		    p.appearance = new DefaultAppearance();
		}));

	detailsArea = new SimpleArea(getControlContext(), s.detailsAreaName());

	setAreaLayout(AreaLayout.TOP_BOTTOM,
		      containersArea, null,
		      detailsArea, null);
    }

    static private final class DefaultAppearance implements ConsoleArea.Appearance<String>
    {
	@Override public void announceItem(String item)
	{
	}

	@Override public String getTextAppearance(String item)
	{
	    return item != null ? item : "";
	}
    }
}