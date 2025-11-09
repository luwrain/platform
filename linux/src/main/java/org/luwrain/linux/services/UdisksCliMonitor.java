// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux.services;

import java.util.*;
import java.util.function.*;
import java.util.regex.*;
import java.io.*;

import org.luwrain.core.*;
import org.luwrain.linux.*;
import org.luwrain.script.core.*;

import static org.luwrain.script.Hooks.*;


public final class UdisksCliMonitor implements BashProcess.Listener, AutoCloseable
{
    static private final String
	LOG_COMPONENT = "udisks";

    static private final Pattern
	RE_ADDED = Pattern.compile("^\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d:\\sAdded\\s(.*)$"),
	RE_REMOVED = Pattern.compile("^\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d:\\sRemoved\\s(.*)$"),
	    	RE_PROP_CHANGED = Pattern.compile("^\\d\\d:\\d\\d:\\d\\d\\.\\d\\d\\d:\\s*([^:]+):\\s*(.*):\\s*Properties Changed\\s*$");

    static private final String
	OBJ_DRIVES = "/org/freedesktop/UDisks2/drives/",
		OBJ_BLOCK = "/org/freedesktop/UDisks2/block_devices/",
	OBJ_MANAGER = "/org/freedesktop/UDisks2/Manager",
	IFACE_DRIVE = "org.freedesktop.UDisks2.Drive",
	IFACE_BLOCK = "org.freedesktop.UDisks2.Block",
	IFACE_FILESYSTEM = "org.freedesktop.UDisks2.Filesystem",
	PREFIX_REMOVABLE = "Removable:",
		PREFIX_EJECTABLE = "Ejectable:",
	PREFIX_SIZE = "Size:",
	PREFIX_MODEL = "Model:",
	PREFIX_VENDOR = "Vendor:",
	PREFIX_DEVICE = "Device:",
	PREFIX_DRIVE = "Drive:",
	PREFIX_FS_TYPE = "IdType:",
	PREFIX_MOUNT_POINTS = "MountPoints:";

    private final Luwrain luwrain;
    private final BashProcess p ;
    private boolean closed = true;
    private final Map<String, Disk> disks = new HashMap<>();
    private final Map<String, BlockDev> blockDevs = new HashMap<>();
    private Disk activeDisk = null;
    private BlockDev activeBlockDev = null;
    private String activeIface = null;

    public UdisksCliMonitor(Luwrain luwrain) throws IOException
    {
	this.luwrain = luwrain;
	init();
	this.p = new BashProcess("udisksctl monitor", null, EnumSet.noneOf(BashProcess.Flags.class), this);
this.p.run();
	    }

    @Override public synchronized void close()
    {
	if (closed)
	    return;
	Log.debug(LOG_COMPONENT, "stopping udisksctl monitor");
	this.closed = true;
	p.stop();
    }

    public synchronized void enumRemovableBlockDevices(Consumer<Map<String, Object>> consumer)
    {
	for(Map.Entry<String, BlockDev> e: blockDevs.entrySet())
	    if (e.getValue().isReady())
	    {
		final Disk disk = disks.get(e.getValue().drive.replaceAll("'", ""));
		if (disk == null)
		    continue;
		final Map<String, Object> m = new HashMap<>(e.getValue().createAttrMap());
		final boolean
		removable = disk.removable != null && disk.removable.toLowerCase().equals("true"),
		ejectable = disk.ejectable != null && disk.ejectable.toLowerCase().equals("true");
		m.put("removable", Boolean.valueOf(removable));
		m.put("ejectable", Boolean.valueOf(ejectable));
	    consumer.accept(m);
	    }
    }

    		@Override public synchronized void onOutputLine(String line)
		{
		    //		    		    Log.debug(LOG_COMPONENT, line);
		    try {
		    Matcher m = RE_ADDED.matcher(line);
		    if (m.find())
		    {
			final String obj = m.group(1).trim();
			activeDisk = null;
			activeBlockDev = null;
			activeIface = null;

			if (obj.startsWith(OBJ_DRIVES))
			{
			    activeDisk = new Disk(obj);
			    disks.put(obj, activeDisk);
			    Log.debug(LOG_COMPONENT, "added new disk: " + obj);
			    return;
			}

						if (obj.startsWith(OBJ_BLOCK))
			{
			    activeBlockDev = new BlockDev(obj);
			    blockDevs.put(obj, activeBlockDev);
			    Log.debug(LOG_COMPONENT, "added new block device: " + obj);
			    return;
			}

			return;
		    }

m = RE_REMOVED.matcher(line);
		    if (m.find())
		    {
			final String obj = m.group(1).trim();
			activeDisk = null;
			activeBlockDev = null;
			activeIface = null;

			if (disks.containsKey(obj))
			{
			    final Disk disk = disks.get(obj);
			    disks.remove(obj);
			    Log.debug(LOG_COMPONENT, "removed disk " + disk.obj);
			    chainOfResponsibility(luwrain, Hooks.DISK_REMOVED, new Object[]{ new MapScriptObject(disk.createAttrMap())});
			    return;
			}

						if (blockDevs.containsKey(obj))
			{
			    final BlockDev blockDev = blockDevs.get(obj);
			    blockDevs.remove(obj);
			    Log.debug(LOG_COMPONENT, "removed block device " + blockDev.obj);
			    chainOfResponsibility(luwrain, Hooks.BLOCK_DEV_REMOVED, new Object[]{ new MapScriptObject(blockDev.createAttrMap())});
			    return;
			}

						return;
		    }

		    //Updating properties
m = RE_PROP_CHANGED.matcher(line);
				    if (m.find())
				    {
					final String
					obj = m.group(1).trim(),
					iface = m.group(2).trim();

					if (obj.startsWith(OBJ_DRIVES) && disks.containsKey(obj))
					{
					    activeDisk = disks.get(obj);
					    activeIface = iface;
					    					    Log.debug(LOG_COMPONENT, "updating the disk " + obj + " with the interface " + activeIface);
					    return;
					}

										if (obj.startsWith(OBJ_BLOCK) && blockDevs.containsKey(obj))
					{
					    activeBlockDev = blockDevs.get(obj);
					    activeIface = iface;
					    Log.debug(LOG_COMPONENT, "updating the block device " + obj + " with the interface " + activeIface);
					    return;
					}

										Log.warning(LOG_COMPONENT, "unrecognized updating line: " + line);
					return;
				    }

		    final String l = line.trim();
		    if (l.startsWith("org.freedesktop") && l.endsWith(":"))
		    {
			activeIface = l.substring(0, l.length() - 1);
			return;
		    }

		    if (activeDisk != null)
			activeDisk.onLine(activeIface, l);
		    if (activeBlockDev != null)
			activeBlockDev.onLine(activeIface, l);
		    }
		    catch(Throwable e)
		    {
			Log.error(LOG_COMPONENT, "unable to process a line of udisksctl output: " + e.getClass().getName() + ": " + e.getMessage());
		    }
		}

    private void init() throws IOException
    {
	final BashProcess p = new BashProcess("udisksctl dump", EnumSet.of(BashProcess.Flags.LOG_OUTPUT));
	p.run();
	final int exitCode = p.waitFor();
	if (exitCode != 0)
	    throw new IOException("udisksctl dump returned exit code " + String.valueOf(exitCode));
	final String[] output = p.getOutput();
	for(String l: output)
	{
	    final String line = l.trim();
	    if (line.startsWith("/org/freedesktop/") && line.endsWith(":"))
	    {
		final String obj = line.substring(0, line.length() - 1);

		//Adding disk
		if (obj.startsWith(OBJ_DRIVES))
		{
		    activeDisk = new Disk(obj);
		    disks.put(obj, activeDisk);
		    activeBlockDev = null;
		    activeIface = null;
		    Log.debug(LOG_COMPONENT, "adding disk " + obj);
		    continue;
		}

				//Adding block device
		if (obj.startsWith(OBJ_BLOCK))
		{
		    activeBlockDev = new BlockDev(obj);
		    blockDevs.put(obj, activeBlockDev);
		    activeDisk = null;
		    activeIface = null;
		    Log.debug(LOG_COMPONENT, "adding block device " + obj);
		    continue;
		}

		if (obj.startsWith(OBJ_MANAGER))
		    continue;

		Log.warning(LOG_COMPONENT, "unrecognized initial object: " + line);
		continue;
	    }

	    if (line.startsWith("org.freedesktop.") && line.endsWith(":"))
	    {
		activeIface = line.substring(0, line.length() - 1);
		continue;
	    }

	    if (activeIface == null)
		continue;

	    if (activeDisk != null)
		activeDisk.onLine(activeIface, line); else
		if (activeBlockDev != null)
		    activeBlockDev.onLine(activeIface, line);
	}
	activeDisk = null;
	activeBlockDev = null;
	activeIface = null;
    }

	@Override public void onErrorLine(String line)
	{
	    Log.error(LOG_COMPONENT, "monitor error: " + line);
	}

    @Override public void onFinishing(int exitCode)
    {
	activeDisk = null;
	activeBlockDev = null;
	disks.clear();
	blockDevs.clear();
	if (!closed)
	{
	    if (exitCode == 0)
		Log.debug(LOG_COMPONENT, "the monitor finished without errors"); else
		Log.error(LOG_COMPONENT, "the monitor finished with the exit code " + String.valueOf(exitCode));
	}
	closed = true;
    }

    private final class Disk
    {
	final String obj;
	String
	    vendor = null,
	    model = null,
	    removable = null,
	    ejectable = null;
	Disk(String obj) { this.obj = obj; }
	void onLine(String iface, String line)
	{
	    if (!iface.equals(IFACE_DRIVE))
		return;
	    if (line.startsWith(PREFIX_MODEL))
		model = line.substring(PREFIX_MODEL.length()).trim();
	    if (line.startsWith(PREFIX_VENDOR))
		vendor = line.substring(PREFIX_VENDOR.length()).trim();
	    	    if (line.startsWith(PREFIX_REMOVABLE))
		removable = line.substring(PREFIX_REMOVABLE.length()).trim();
		    	    	    if (line.startsWith(PREFIX_EJECTABLE))
				    {
		ejectable = line.substring(PREFIX_EJECTABLE.length()).trim();
				    }
	    /*
	    if (isReady())
		chainOfResponsibility(luwrain, Hooks.DISK_ADDED, new Object[]{new MapScriptObject(createAttrMap())});
	    */
	}
	boolean isReady()
	{
	    return
	    model != null &&
	    vendor != null &&
removable != null &&
	    ejectable != null;
	}
	Map<String, Object> createAttrMap()
	{
	    final Map<String, Object> d = new HashMap<>();
	    d.put("obj", obj);
	    d.put("model", model);
	    d.put("vendor", vendor);
	    return d;
	}
    }

    private final class BlockDev
    {
	final String obj;
	String
	    device = null,
	    drive = null,
	    fsType = null,
	    mountPoints = "";
	BlockDev(String obj) { this.obj = obj; }
	void onLine(String iface, String line)
	{
	    if (line.startsWith(PREFIX_DEVICE))
		device = line.substring(PREFIX_DEVICE.length()).trim();
	    if (line.startsWith(PREFIX_DRIVE))
		drive = line.substring(PREFIX_DRIVE.length()).trim();
	    if (line.startsWith(PREFIX_FS_TYPE))
		fsType = line.substring(PREFIX_FS_TYPE.length()).trim();
	    if (iface.equals(IFACE_FILESYSTEM) && line.startsWith(PREFIX_MOUNT_POINTS))
		mountPoints = line.substring(PREFIX_MOUNT_POINTS.length()).trim();
	    /*
	    if (isReady())
		chainOfResponsibility(luwrain, Hooks.BLOCK_DEV_ADDED, new Object[]{new MapScriptObject(createAttrMap())});
	    */
	}
	boolean isReady()
	{
	    return device != null && drive != null && fsType != null/* && mountPoints != null*/;
	}
	Map<String, Object> createAttrMap()
	{
	    final Map<String, Object> d = new HashMap<>();
	    d.put("obj", obj);
	    d.put("device", device);
	    d.put("drive", drive);
	    d.put("fsType", fsType);
	    d.put("mountPoints", mountPoints);
	    return d;
	}
    }
}
