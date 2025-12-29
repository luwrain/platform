// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import java.io.*;
import java.util.*;
import org.apache.logging.log4j.*;

import static java.util.Objects.*;
import static org.luwrain.util.LineIterator.*;

public final class BlockDevices
{
    static private final Logger log = LogManager.getLogger();
    static public final File
	SYS_BLOCK = new File("/sys/block"),
	DEV = new File("/dev");

    public String[] getHardDrives()
    {
	final List<String> res = new ArrayList<>();
	final File[] dev = SYS_BLOCK.listFiles();
	if (dev == null)
	    return new String[0];
	final File devDir = new File("/dev");
	for(File f: dev)
	{
	    if (!f.isDirectory())
		continue;
	    final File removable = new File(f, "removable");
	    final File cap = new File(f, "capability");
	    try {
		if (new File(f, "loop").exists())
		    continue;
		if (new File(f, "dm").exists()) //Appears on crypt devices
		    continue;
		if (join(removable, UTF_8, System.lineSeparator()).trim().equals("1"))
		    continue;
		final Integer capValue = Integer.parseInt(join(cap, UTF_8, System.lineSeparator()).trim(), 16);
		if ((capValue.intValue() & 0x0010) == 0) //The device is down
		    continue;
		if ((capValue.intValue() & 0x0200) != 0) //Partition scanning is disabled. Used for loop devices in their default settings
		    continue;
	    }
	    catch(Exception e)
	    {
		log.trace("Exploring " + f.getAbsolutePath(), e);
		continue;
	    }
	    res.add(f.getName());
	}
	final String[] r = res.toArray(new String[res.size()]);
	Arrays.sort(r);
	return r;
    }

    public String getDeviceName(String dev)
    {
	requireNonNull(dev, "dev can't be null");
	try {
	    return join(new File(new File(SYS_BLOCK, dev), "device/model"), "UTF-8", System.lineSeparator()).trim();
	}
	catch(IOException e)
	{
	    log.error("Unable to get the name of the device " + dev, e);
	    return "";
	}
    }

    public long getDeviceSize(String dev)
    {
	requireNonNull(dev, "dev can't be null");
	try {
	    final String sizeStr = join(new File(new File(SYS_BLOCK, dev), "size"), UTF_8, System.lineSeparator()).trim();
	    final Long l = Long.parseLong(sizeStr);
	    return l.longValue() * 512;
	}
	catch(IOException e)
	{
	    log.error("Unable to get the name of the device " + dev, e);
	    return -1;
	}
    }
}
