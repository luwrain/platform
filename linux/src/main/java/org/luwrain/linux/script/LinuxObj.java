// SPDX-License-Identifier: BUSL-1.1
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

import static org.luwrain.script.ScriptUtils.*;

final class LinuxObj implements ProxyObject
{
    static private String[] KEYS = new String[]{
	"run",
	"runAsync",
    };
    static private final Set<String> KEYS_SET = new HashSet<>(Arrays.asList(KEYS));
    static private final ProxyArray KEYS_ARRAY = ProxyArray.fromArray((Object[])KEYS);

    final Luwrain luwrain;
    final Object syncObj;

    LinuxObj(Luwrain luwrain, Object syncObj)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(syncObj, "syncObj");
	this.luwrain = luwrain;
	this.syncObj = syncObj;
    }

    @Override public Object getMember(String name)
    {
	if (name == null)
	    return null;
	switch(name)
	{
	case "run":
	    return(ProxyExecutable)this::run;
	case "runAsync":
	    return(ProxyExecutable)this::runAsync;
	default:
	    return null;
	}
    }

    @Override public boolean hasMember(String name) { return KEYS_SET.contains(name); }
    @Override public Object getMemberKeys() { return KEYS_ARRAY; }
    @Override public void putMember(String name, Value value) { throw new RuntimeException("The Linux object doesn't support updating of its variables"); }

    private Object run(Value[] args)
    {
	if (!notNullAndLen(args, 1))
	    throw new ScriptException("Linux.run() takes exactly one argument with the command line to run");
	final String command = ScriptUtils.asString(args[0]);
	if (command == null)
	    throw new ScriptException("Linux.run() takes a not null string as its first argument");
	final BashProcess p = new BashProcess(command);
	try {
	    p.run();
	}
	catch(IOException e)
	{
	    throw new ScriptException(e);
	}
	p.waitFor();
	return new BashProcessObj(p);
    }

    private Object runAsync(Value[] args)
    {
	if (!notNull(args))
	    throw new ScriptException("Linux.runAsync() doesn't take null in arguments");
	if (args.length < 1 || args.length > 3)
	    throw new ScriptException("Linux.runAsync() takes 1, 2 or 3 arguments");
	if (!args[0].isString())
	    throw new ScriptException("Linux.runAsync() takes a string as its first argument");
	final String cmd = args[0].asString();
	final Value output, error;
	if (args.length >= 2)
	{
	    if (!args[1].canExecute())
		throw new ScriptException("Linux.runAsync() takes a function as its second argument");
	    output = args[1];
	} else
	    output = null;
	if (args.length == 3)
	{
	    if (!args[2].canExecute())
		throw new ScriptException("Linux.runAsync() takes a function as its third argument");
	    error = args[2];
	} else
	    error = null;
	final BashProcess.Listener listener = new BashProcess.Listener(){
		@Override public void onOutputLine(String line)
		{
		    if (output == null)
			return;
		    synchronized(syncObj) {
			output.execute(line);
		    }
		}
		@Override public void onErrorLine(String line)
		{
		    if (error == null)
			return;
		    synchronized(syncObj) {
			error.execute(line);
		    }
		}
		@Override public void onFinishing(int exitCode)
		{
		}
	    };
	final BashProcess p = new BashProcess(cmd, null, EnumSet.noneOf(BashProcess.Flags.class), listener);
	try {
	    p.run();
	}
	catch(IOException e)
	{
	    throw new ScriptException(e);
	}
	return new BashProcessObj(p);
    }
}
