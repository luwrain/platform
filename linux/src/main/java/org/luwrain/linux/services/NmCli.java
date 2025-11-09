// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux.services;

import java.util.*;
import java.io.*;

import org.luwrain.core.*;
import org.luwrain.linux.*;

public final class NmCli
{
    static private final String
	LOG_COMPONENT = "nmcli",
	IN_USE = "IN-USE:",
	SECURITY = "SECURITY:",
	SSID = "SSID:",
	SIGNAL = "SIGNAL:";

    public interface Caller
    {
	String[] call(String[] args) throws IOException;
    }

    static private final class Network implements WifiNetwork
    {
	private final String name, protectionType;
	private boolean connected = false;
	private final int signalLevel;
	Network(String name, String protectionType, boolean connected, int signalLevel)
	{
	    NullCheck.notNull(name, "name");
	    NullCheck.notNull(protectionType, "protectionType");
	    this.name = name;
	    this.protectionType = protectionType;
	    this.signalLevel = signalLevel;
	    this.connected = connected;
	}
	@Override public String getName() { return name; }
	@Override public String getProtectionType() { return protectionType; }
	@Override public boolean isConnected() { return connected; }
	@Override public int getSignalLevel() { return signalLevel; }
	@Override public String toString() { return name; }
    }

    private final Caller caller;

    public NmCli(Caller caller)
    {
	NullCheck.notNull(caller, "caller");
	this.caller = caller;
    }

    public NmCli()
    {
	this(createDefaultCaller());
    }

    public WifiNetwork[] scan() throws IOException
    {
	final List<WifiNetwork> res = new ArrayList<>();
	final String[] lines = caller.call(new String[]{"device", "wifi", "list"});
	String
	ssid = null,
	security = null,
	inUse = null,
	signal = null;
	for (String l: lines)
	{
	    final String line = l.trim();
	    if (line.startsWith(IN_USE))
		inUse = line.substring(IN_USE.length()).trim();
	    if (line.startsWith(SSID))
		ssid = line.substring(SSID.length()).trim();
	    if (line.startsWith(SIGNAL))
		signal = line.substring(SIGNAL.length()).trim();
	    if (line.startsWith(SECURITY))
		security = line.substring(SECURITY.length()).trim();
	    if (ssid != null && inUse != null && signal != null && security != null)
	    {
		int level = 0;
		try {
		    level = Integer.parseInt(signal);
		}
		catch(NumberFormatException e)
		{
		    Log.warning(LOG_COMPONENT, "unparsable signal level value: " + signal);
		}
		res.add(new Network(ssid, security.trim(), !inUse.trim().isEmpty(), level));
		inUse = null;
		ssid = null;
		security = null;
		signal = null;
	    }
	}
	return res.toArray(new WifiNetwork[res.size()]);
    }

    public boolean connect(WifiNetwork network, String password) throws IOException
    {
	try {
	    if (password != null && !password.isEmpty())
	    caller.call(new String[]{"device", "wifi", "connect", network.getName(), "password", password}); else
			    caller.call(new String[]{"device", "wifi", "connect", network.getName()});
	    return true;
	}
	catch(Throwable e)
	{
	    Log.error(LOG_COMPONENT, "unable to connect to the network '" + network.getName() + "': " + e.getClass().getName() + ": " + e.getMessage());
	    return false;
	}
    }

        public boolean disconnect() throws IOException
    {
	return false;
    }

    static public Caller createDefaultCaller()
    {
	return (args)->{
	    final StringBuilder cmd = new StringBuilder();
	    cmd.append("nmcli -m multiline");
	    if (args != null)
		for(String a: args)
		    cmd.append(" ").append(BashProcess.escape(a));
	    final BashProcess p = new BashProcess(new String(cmd), EnumSet.of(BashProcess.Flags.ROOT, BashProcess.Flags.LOG_OUTPUT));
	    p.run();
	    final int exitCode = p.waitFor();
	    if (exitCode != 0)
		throw new IOException("nmcli returned " + String.valueOf(exitCode));
	    return p.getOutput();
	};
    }
}
