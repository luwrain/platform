// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux.script;

import org.graalvm.polyglot.*;

import org.luwrain.core.*;

public final class Bindings implements org.luwrain.script.core.Bindings
{
    private final Luwrain luwrain;

    public Bindings(Luwrain luwrain)
    {
	NullCheck.notNull(luwrain, "luwrain");
	this.luwrain = luwrain;
    }

    @Override public void onBindings(Value value, Object syncObj)
    {
	NullCheck.notNull(value, "value");
	NullCheck.notNull(syncObj, "syncObj");
	value.putMember("Linux", new LinuxObj(luwrain, syncObj));
    }
}
