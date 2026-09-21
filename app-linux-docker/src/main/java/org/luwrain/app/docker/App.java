// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2026 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.app.docker;

import java.util.*;

import org.luwrain.core.*;
import org.luwrain.app.base.*;
import org.luwrain.core.annotations.*;

@AppNoArgs(name = "docker",
	   title = {"en=Docker", "ru=Docker"})
public final class App extends AppBase<Strings>
{
    Conv conv = null;
    MainLayout mainLayout = null;

    public App()
    {
	super(Strings.class, "luwrain.docker");
    }

    @Override public AreaLayout onAppInit()
    {
	conv = new Conv(this);
	mainLayout = new MainLayout(this);
	setAppName(getStrings().appName());
	return mainLayout.getAreaLayout();
    }

    @Override public boolean onEscape()
    {
	closeApp();
	return true;
    }
}