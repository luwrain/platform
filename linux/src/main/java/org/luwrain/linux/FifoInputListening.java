// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import java.io.*;
import java.util.concurrent.*;
import org.luwrain.core.events.*;

import org.luwrain.core.*;

final class FifoInputListening
{
    static private final String LOG_COMPONENT = Linux.LOG_COMPONENT;

    static private final String COMMAND_PREFIX = "command ";
    static private final String USB_DISK_ATTACHED_PREFIX = "usbdiskattached ";
        static private final String CDROM_CHANGED_PREFIX = "cdromchanged ";
        static private final String UNIREF_PREFIX = "uniref ";

    private final Luwrain luwrain;
    private final Linux linux;
    private final String fileName;
    private FutureTask task = null;

    FifoInputListening(Luwrain luwrain, Linux linux, String fileName)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notNull(linux, "linux");
	NullCheck.notEmpty(fileName, "fileName");
	this.luwrain = luwrain;
	this.linux = linux;
	this.fileName = fileName;
    }

    void run()
    {
	task = createTask();
	Log.debug(LOG_COMPONENT, "starting fifo input listening on " + fileName);
	luwrain.executeBkg(task);
    }

    private FutureTask createTask()
    {
	return new FutureTask<>(()->{
		BufferedReader reader = null;
		FileOutputStream output = null;
		try {
		    reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
		    output = new FileOutputStream(fileName);
		    String line = null;
		    do {
			line = reader.readLine();
			if (line != null)
			    processLine(line);
		    } while(line != null);
		}
		catch(Exception e)
		{
		    try {
			if (reader != null)
			    reader.close();
			if (output != null)
			    output.close();
		    }
		    catch(IOException ee)
		    {
		    }
		    Log.error(LOG_COMPONENT, "unable to get pointer input events:" + e.getClass().getName() + ":" + e.getMessage());
		}
	}, null);
    }

    private void processLine(String line)
    {
	NullCheck.notNull(line, "line");

	//command
	if (line.startsWith(COMMAND_PREFIX))
	{
	    final String command = line.substring(COMMAND_PREFIX.length()).trim();
	    if (!command.isEmpty())
	luwrain.runUiSafely(()->{
		luwrain.runCommand(command);
	    });
	return;
	}

	//usb disk
	/*
		if (line.startsWith(USB_DISK_ATTACHED_PREFIX))
	{
	    final String path = line.substring(USB_DISK_ATTACHED_PREFIX.length()).trim();
	    if (!path.isEmpty())
		luwrain.runUiSafely(()->linux.onUsbDiskAttached(luwrain, path));
	return;
	}
	*/

			//cdrom
	/*
		if (line.startsWith(CDROM_CHANGED_PREFIX))
	{
	    final String path = line.substring(CDROM_CHANGED_PREFIX.length()).trim();
	    if (!path.isEmpty())
		luwrain.runUiSafely(()->linux.onCdromChanged(luwrain, path));
	return;
	}
	*/



		//uniref
		if (line.startsWith(UNIREF_PREFIX))
	{
	    final String uniref = line.substring(UNIREF_PREFIX.length()).trim();
	    if (!uniref.isEmpty())
	luwrain.runUiSafely(()->{
		luwrain.openUniRef(uniref);
	    });
	return;
	}
    }
}
