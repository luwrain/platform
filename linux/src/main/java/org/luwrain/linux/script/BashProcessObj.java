// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux.script;

import java.io.*;
import java.util.*;

import org.graalvm.polyglot.*;
import org.graalvm.polyglot.proxy.*;

import org.luwrain.core.*;
import org.luwrain.script.core.*;
import org.luwrain.script.*;
import org.luwrain.linux.*;

final class BashProcessObj implements ProxyObject
{
    private final BashProcess p;

    BashProcessObj(BashProcess p)
    {
	NullCheck.notNull(p, "p");
	this.p = p;
    }

    @Override public Object getMember(String name)
    {
	if (name == null)
	    return null;
	switch(name)
	{
	case "output":
	    return ProxyArray.fromArray((Object[])p.getOutput());
	    	case "errors":
		    return ProxyArray.fromArray((Object[])p.getErrors());
	    	    	case "waitFor":
			    return (ProxyExecutable)this::waitFor;
	default:
	    return null;
	}
    }

    @Override public boolean hasMember(String name)
    {
	switch(name)
	{
	case "output":
	case "errors":
	case "waitFor":
	    return true;
	    	default:
	    return false;
	}
    }

    @Override public Object getMemberKeys()
    {
	return new String[]{
	    "output",
	    "errors",
	    "waitFor",
	};
    }

    @Override public void putMember(String name, Value value)
    {
	throw new ScriptException("The bash process object doesn't support updating of its variables");
    }

    private Object waitFor(Value[] values)
    {
	return p.waitFor();
    }
    }
