// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>
// Copyright 2022 ilya paschuk <ilusha.paschuk@gmail.com>

package org.luwrain.linux.services;

import java.util.*;
//import java.util.function.*;
import java.util.regex.*;
import java.io.*;

import org.luwrain.core.*;
import org.luwrain.linux.*;
//import org.luwrain.script.core.*;

import static org.luwrain.script.Hooks.*;

public final class UdisksCli
{
    static private final String
	LOG_COMPONENT = "udisks";

    static private final Pattern
	PAT_MOUNTED = Pattern.compile("^\\s*Mounted\\s+[^ ]+\\s+at\\s+([^ ].*)\\s*$");

    public interface Caller
    {
	String[] call(String[] args) throws IOException;
    }

    private final Caller caller;

    public UdisksCli(Caller caller)
    {
	NullCheck.notNull(caller, "caller");
	this.caller = caller;
    }

    public UdisksCli()
    {
	this(createDefaultCaller());
    }

    public File mount(String device) throws IOException
    {
	final String[] res = caller.call(new String[]{"mount", "-b", device});
	if (res.length != 1)
	{
	    Log.error(LOG_COMPONENT, "illegal number of output lines from udisksctl mount: " + Arrays.toString(res));
	    return null;
	}
	final Matcher m = PAT_MOUNTED.matcher(res[0]);
	if (!m.find())
	{
	    Log.error(LOG_COMPONENT, "unrecognized udisksctl mount output line: " + res[0]);
	    return null;
	}
	return new File(m.group(1));
    }

    public void unmount(String device) throws IOException
    {
	caller.call(new String[]{"unmount", "-b", device});
    }

public void poweroff(String device) throws IOException
    {
	caller.call(new String[]{"power-off", "-b", device});
    }


    static public Caller createDefaultCaller()
    {
	return (args)->{
	    final StringBuilder cmd = new StringBuilder();
	    cmd.append("udisksctl");
	    if (args != null)
		for(String a: args)
		    cmd.append(" ").append(BashProcess.escape(a));
	    final BashProcess p = new BashProcess(new String(cmd));
	    p.run();
	    final int exitCode = p.waitFor();
	    if (exitCode != 0)
		throw new IOException("nmcli returned " + String.valueOf(exitCode));
	    return p.getOutput();
	};
    }
}
