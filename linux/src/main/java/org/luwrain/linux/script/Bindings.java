// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux.script;

import org.graalvm.polyglot.*;

import org.luwrain.core.*;
import static java.util.Objects.*;

public final class Bindings implements org.luwrain.script.core.Bindings
{
    private final Luwrain luwrain;

    public Bindings(Luwrain luwrain)
    {
	requireNonNull(luwrain, "luwrain can't be null");
	this.luwrain = luwrain;
    }

    @Override public void onBindings(Value value, Object syncObj)
    {
	requireNonNull(value, "value can't be null");
	requireNonNull(syncObj, "syncObj can't be null");
	value.putMember("Linux", new LinuxObj(luwrain, syncObj));
    }
}
