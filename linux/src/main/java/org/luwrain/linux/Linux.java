// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import java.util.*;
import java.io.*;
import java.nio.file.*;
import com.google.auto.service.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;

@AutoService(org.luwrain.core.OperatingSystem.class)
public final class Linux implements OperatingSystem
{
    static public final String
	LOG_COMPONENT = "linux";

    static public final Syscalls syscalls = new Syscalls();
    private PropertiesBase props = null;

    @Override public InitResult init(PropertiesBase props)
    {
	Extension.setLinux(this);
	this.props = props;
	return new InitResult();
    }

    @Override public String escapeString(String style, String value)
    {
	switch(style.trim().toUpperCase())
	{
	case "CMD":
	case "SHELL":
	case "BASH":
	    return BashProcess.escape(value);
	default:
	    Log.warning(LOG_COMPONENT, "unknown escaping style: " + style);
	    return value;
	}
    }

    public String getProperty(String propName)
    {
	NullCheck.notNull(propName, "propName");
	return "";
    }

    @Override public Braille getBraille()
    {
	return new BrlApi();
    }

    @Override public void openFileInDesktop(Path path)
    {
	throw new UnsupportedOperationException("Linux has no support of opening files in desktop environment");
    }

    @Override public org.luwrain.interaction.KeyboardHandler getCustomKeyboardHandler(String subsystem)
    {
	NullCheck.notNull(subsystem, "subsystem");
	switch(subsystem.toLowerCase().trim())
	{
	case "javafx":
	    return new KeyboardJavafxHandler();
	default:
	    return null;
	}
    }

    PropertiesBase getProps()
    {
	return props;
    }
}
