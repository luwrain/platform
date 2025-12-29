// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

import org.apache.logging.log4j.*;

import static java.util.Objects.*;

final class PciIds
{
    static private final Logger log = LogManager.getLogger();

    static private final Pattern
	VENDOR_PATTERN = Pattern.compile("^([^\\s]+)\\s+([^\\s].*)$"),
	CLASS_PATTERN = Pattern.compile("^C\\s*([^\\s]+)\\s+([^\\s].*)$"),
	DEVICE_PATTERN = Pattern.compile("^\t([^\\s]+)\\s+([^\\s].*)$");

    private final Map<String, Vendor> vendors = new TreeMap<String, Vendor>();
    private final Map<String, Class> classes = new TreeMap<String, Class>();
    private Vendor lastVendor = null;
    private Class lastClass = null;

    String findVendor(String code)
    {
	requireNonNull(code, "code can't be null");
	if (code.isEmpty())
	    throw new IllegalArgumentException("code can't be empty");
	if (!vendors.containsKey(code))
	    return null;
	return vendors.get(code).name;
    }

    String findDevice(String vendorCode, String deviceCode)
    {
	requireNonNull(vendorCode, "vendorCode can't be null");
	requireNonNull(deviceCode, "deviceCode can't be null");
	if (!vendors.containsKey(vendorCode))
	    return null;
	final Vendor v = vendors.get(vendorCode);
	if (!v.devices.containsKey(deviceCode))
	    return null;
	return v.devices.get(deviceCode).name;
    }

    String findClass(String classCode)
    {
	requireNonNull(classCode, "classCode can't be null");
	if (classCode.length() < 2)
	    return null;
	if (!classes.containsKey(classCode.substring(0, 2)))
	    return null;
	final Class c = classes.get(classCode.substring(0, 2));
	return c.name;
	//	if (!v.devices.containsKey(deviceCode))
	//	    return null;
	//	return v.devices.get(deviceCode).name;
    }

    void load(File file)
    {
	try  {
	    final InputStream is = new FileInputStream(file);
	    try {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		String line = null;
		while ( (line = reader.readLine()) != null)
		    onLine(line);
	    }
	    finally {
		is.close();
	    }
	}
	catch(Exception e)
	{
	    log.error("Unable to read PCIIDs from", e);
	}
    }

    private void onVendor(String line)
    {
	requireNonNull(line, "line can't be null");
	final Matcher matcher = VENDOR_PATTERN.matcher(line);
	if (!matcher.find())
	    return;
	final Vendor v = new Vendor(matcher.group(2));
	vendors.put(matcher.group(1).trim(), v);
	lastVendor = v;
	lastClass = null;
    }

    private void onClass(String line)
    {
	requireNonNull(line, "line can't be null");
	final Matcher matcher = CLASS_PATTERN.matcher(line);
	if (!matcher.find())
	    return;
	final Class c = new Class(matcher.group(2));
	classes.put(matcher.group(1).trim(), c);
	lastVendor = null;
	lastClass = c;
    }

    private void onDevice(String line)
    {
	requireNonNull(line, "line can't be null");
	final Matcher matcher = DEVICE_PATTERN.matcher(line);
	if (!matcher.find())
	    return;
	final Device d = new Device(matcher.group(2));
	requireNonNull(lastVendor, "lastVendor can't be null");
	lastVendor.devices.put(matcher.group(1).trim(), d);
    }

    private void onLine(String line)
    {
	if (line.length() < 2 || line.charAt(0) == '#')
	    return;
	if (Character.toLowerCase(line.charAt(0)) == 'c')
	    onClass(line); else
	    if (line.charAt(0) != '\t')
		onVendor(line); else
		if (lastVendor != null && line.charAt(1) != '\t')
		    onDevice(line);
    }

    static private final class Device
    {
	final String name;
	Device(String name)
	{
	    requireNonNull(name, "name can't be null");
	    this.name = name;
	}
    }

    static private final class Subclass
    {
	final String name;
	Subclass(String name)
	{
	    requireNonNull(name, "name can't be null");
	    this.name = name;
	}
    }

    static private final class Vendor
    {
	final String name;
	final TreeMap<String, Device> devices = new TreeMap<String, Device>();
	Vendor(String name)
	{
	    requireNonNull(name, "name can't be null");
	    this.name = name;
	}
    }

    static private final class Class
    {
	final String name;
	final TreeMap<String, Subclass> subclasses = new TreeMap<String, Subclass>();
	Class(String name)
	{
	    requireNonNull(name, "name can't be null");
	    this.name = name;
	}
    }
}
