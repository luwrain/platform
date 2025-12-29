// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import java.io.*;
import java.util.*;

import org.luwrain.core.*;
import org.luwrain.util.*;

import static org.luwrain.util.LineIterator.*;

public final class SysDevicesList
{
    static private final String LOG_COMPONENT = Linux.LOG_COMPONENT;

        private final PropertiesBase props;
        private final PciIds pciIds = new PciIds();
    private final File sysBlockDir;
    private final File pciDevDir;

    public SysDevicesList(PropertiesBase props)
    {
	NullCheck.notNull(props, "props");
	this.props = props;
	final File pciidsFile = props.getFileProperty("luwrain.linux.pciids");
	if (pciidsFile != null)
	pciIds.load(pciidsFile);
	this.sysBlockDir = props.getFileProperty("luwrain.linux.sysblockdir");
	if (sysBlockDir == null)
	    Log.warning(LOG_COMPONENT, "no \'luwrain.linux.sysblockdir\' property");
	this.pciDevDir = props.getFileProperty("luwrain.linux.pcidevdir");
	if (pciDevDir == null)
	    Log.warning(LOG_COMPONENT, "no \'luwrain.linux.pcidevdir\' property");
    }

    public SysDevice[] getSysDevices()
    {
	if (pciDevDir == null)
	    return new SysDevice[0];
	final List<SysDevice> devices = new ArrayList<>();
	final File[] pciDirs = pciDevDir.listFiles();
	if (pciDirs == null)
	    return new SysDevice[0];
	for(File d: pciDirs)
	{
	    final String classStr = readTextFile(new File(d, "class").getAbsolutePath());
	    final String vendorStr = readTextFile(new File(d, "vendor").getAbsolutePath());
	    final String modelStr = readTextFile(new File(d, "device").getAbsolutePath());
	    final SysDevice.Type type = SysDevice.Type.PCI;
	    final String name = d.getName();
	    final String cls;
	    if (classStr != null && classStr.startsWith("0x"))
	    {
		final String res = pciIds.findClass(classStr.substring(2)); 
		if (res != null && !res.isEmpty())
		    cls = res; else
		    cls = classStr;
	    } else
		cls = classStr;
	    final String vendor;
	    if (vendorStr != null && vendorStr.startsWith("0x"))
	    {
		final String res = pciIds.findVendor(vendorStr.substring(2)); 
		if (res != null && !res.isEmpty())
		    vendor = res; else
		    vendor = vendorStr;
	    } else
		vendor = vendorStr;
	    final String model;
	    if (vendorStr != null && vendorStr.startsWith("0x") &&
		modelStr != null && modelStr.startsWith("0x"))
	    {
		final String res = pciIds.findDevice(vendorStr.substring(2), modelStr.substring(2));
		if (res != null && !res.isEmpty())
		    model = res; else
		    model = modelStr;
	    } else
		model = modelStr;
	    devices.add(new SysDevice(type,
				      name,
				      cls,
				      vendor,
				      model,
				      "", //driver
				      "" //module
				      ));
	}
	return devices.toArray(new SysDevice[devices.size()]);
    }

    static private String     readTextFile(String fileName)
    {
	try {
	    final String text = join(new File(fileName), UTF_8, System.lineSeparator());
	    return text.replaceAll("\n", "");
	}
	catch(IOException e)
	{
	    Log.error(LOG_COMPONENT, "unable to read " + fileName + ":" + e.getClass().getName() + ":" + e.getMessage());
	    return "";
	}
    }
}
