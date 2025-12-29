// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import java.util.*;
import java.io.*;

import org.apache.logging.log4j.*;

import org.luwrain.core.*;
import org.luwrain.linux.services.*;
import org.luwrain.popups.*;

import static java.util.Objects.*;

public final class DefaultDisksPopupFactory implements DisksPopup.Factory
{
    static private final Logger log = LogManager.getLogger();

    private final UdisksCliMonitor monitor;
    DefaultDisksPopupFactory(UdisksCliMonitor monitor) { this.monitor = monitor; }

    @Override public DisksPopup.Disks newDisks(Luwrain luwrain)
    {
	return new DisksImpl(monitor);
    }

    static final class DisksImpl implements DisksPopup.Disks
    {
	final UdisksCliMonitor monitor;
	DisksImpl(UdisksCliMonitor monitor) { this.monitor = requireNonNull(monitor, "monitor can't be null"); }
	@Override public DisksPopup.Disk[] getDisks(Set<DisksPopup.Flags> flags)
	{
	    final List<DiskImpl> res = new ArrayList<>();
	    monitor.enumRemovableBlockDevices((m)->{
		    final String
		    obj = m.containsKey("obj")?m.get("obj").toString():"",
		    device = m.containsKey("device")?m.get("device").toString():"",
		    fsType = m.containsKey("fsType")?m.get("fsType").toString():"",
		    mountPoints = m.containsKey("mountPoints")?m.get("mountPoints").toString():"";
		    final boolean
		    ejectable = m.containsKey("ejectable")?((Boolean)m.get("ejectable")).booleanValue():false,
		    removable = m.containsKey("removable")?((Boolean)m.get("removable")).booleanValue():false;
		    if (removable && !fsType.trim().isEmpty())
			res.add(new DiskImpl(device, mountPoints));
		});
	    return res.toArray(new DisksPopup.Disk[res.size()]);
	}
    }

    static final class DiskImpl implements DisksPopup.Disk
    {
	final String
	    title, device;
	private String mountPoint = null;
	DiskImpl(String device, String mountPoint)
	{
	    this.title = device.startsWith("/dev/")?device.substring(5):device;
	    this.device = device;
	    this.mountPoint = mountPoint;
	}
	@Override public boolean isActivated()
	{
	    return mountPoint != null && !mountPoint.trim().isEmpty();
	}
	@Override public File activate(Set<DisksPopup.Flags> flags)
	{
	    if (mountPoint != null && !mountPoint.trim().isEmpty())
		return new File(mountPoint);
	    final UdisksCli u = new UdisksCli();
	    try {
		return u.mount(device);
	    }
	    catch(Throwable e)
	    {
		throw new RuntimeException(e);
	    }
	}
	@Override public boolean deactivate(Set<DisksPopup.Flags> flags)
	{
	    final UdisksCli u = new UdisksCli();
	    try {
		u.unmount(device);
		mountPoint = null;
		return true;
	    }
	    catch(Throwable e)
	    {
		log.error(e);
		throw new RuntimeException(e);
	    }
	}

@Override public boolean poweroff(Set<DisksPopup.Flags> flags)
	{
	    final UdisksCli u = new UdisksCli();
	    try {
		u.poweroff(device);
		return true;
	    }
	    catch(Throwable e)
	    {
		log.error(e);
		throw new RuntimeException(e);
	    }
	}

	@Override public String toString()
	{
	    return title;
	}
    }
}
