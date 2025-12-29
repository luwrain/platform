// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.luwrain.core.*;

import static org.luwrain.util.FileUtils.*;

public final class Parted
{
    static public final String
	GPT= "gpt",
	NVME = "nvme",
	SCSI = "scsi";

    static private final Pattern PAT_DEVICE = Pattern.compile("^(/[^:]*):([^:]*):([^:]*):([^:]*):([^:]*):([^:]*):([^:]*):.*;$", Pattern.CASE_INSENSITIVE);

    public interface Caller
    {
	String[] call(String[] args) throws IOException;
    }

    private final String device;
    private final Caller caller;
    private String size = "", type = "", partTableType = "", name = "";

    public Parted(String device, Caller caller)
    {
	NullCheck.notEmpty(device, "device");
	NullCheck.notNull(caller, "caller");
	this.device = device;
	this.caller = caller;
    }

    public Parted(String device)
    {
	this(device, createDefaultCaller());
    }

    public void init() throws IOException
    {
	final String[] lines = caller.call(new String[]{device, "print"});
	for(String line: lines)
	{
	    final Matcher m = PAT_DEVICE.matcher(line.trim());
	    if (!m.find())
		continue;
	    this.size = m.group(2);
	    this.type = m.group(3);
	    this.partTableType = m.group(6);
	    this.name = m.group(7);
	}
    }

    @Override public String toString()
    {
	final String devStr = BlockDevices.DEV.getPath();
	final StringBuilder b = new StringBuilder();
	if (device.startsWith(devStr))
	    b.append(device.substring(devStr.length())); else
	    b.append(device);
	if (size != null && !size.trim().isEmpty())
	    b.append(", ").append(size.trim());
	if (name != null && !name.trim().isEmpty())
	    b.append(", ").append(name.trim());
	return new String(b);
    }

    public String getSize() { return size; }
    public String getType() { return type; }
    public String getPartTableType() { return partTableType; }
    public String getName() { return name; }

        static public Caller createDefaultCaller()
    {
	return (args)->{
	    final StringBuilder cmd = new StringBuilder();
	    cmd.append("parted -m");
	    if (args != null)
		for(String a: args)
		    cmd.append(" ").append(BashProcess.escape(a));
	    final BashProcess p = new BashProcess(new String(cmd), EnumSet.of(BashProcess.Flags.ROOT));
	    p.run();
	    final int exitCode = p.waitFor();
	    if (exitCode != 0)
		throw new IOException("nmcli returned " + String.valueOf(exitCode));
	    return p.getOutput();
	};
    }

}
