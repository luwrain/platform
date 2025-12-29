// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.io.*;

public final class ShellCmd
{
    public final Process p;
    public final List<String>
	output = new ArrayList<>(),
	error = new ArrayList<>();
    private final AtomicBoolean
	outputComplete = new AtomicBoolean(false),
	errorComplete = new AtomicBoolean(false);

    public ShellCmd(String cmd, String dir)
    {
	try {
	p = new ProcessBuilder("/bin/bash", "-c", cmd)
	.directory(new File(dir))
	.start();
	}
	catch(IOException ex)
	{
	    //FIXME: logging
	    throw new RuntimeException(ex);
	}
    }

    public int waitFor()
    {
	try {
	p.getOutputStream().close();
	}
	catch(IOException ex)
	{
	    //FIXME: log
	    throw new RuntimeException(ex);
	}
	new Thread(() -> readStream(p.getInputStream(), output, outputComplete)).start();
	new Thread(() -> readStream(p.getErrorStream(), error, errorComplete)).start();
	try {
	    p.waitFor();
	    synchronized(this) {
		while (!outputComplete.get() || !errorComplete.get())
		    wait();
	    }
	    return p.exitValue();
	}
	catch(InterruptedException ex)
	{
	    Thread.currentThread().interrupt();
	}
	return -1;
    }

    private void readStream(InputStream s, List<String> lines, AtomicBoolean complete)
    {
	try {
		try {
		    try (final var r = new BufferedReader(new InputStreamReader(s, "UTF-8"))) {
			for(var line = r.readLine();line != null;line = r.readLine())
			    lines.add(line);
	}
		}
	finally {
	    s.close();
	}
	}
	catch(IOException ex)
	{
	    //FIXME: log
	    ex.printStackTrace();
	}
		    synchronized(this)
		    {
	    complete.set(true);
	    notify();
	}
    }

    static public int execAndWait(String cmd, String dir)
    {
	return new ShellCmd(cmd, dir).waitFor();
    }
}
