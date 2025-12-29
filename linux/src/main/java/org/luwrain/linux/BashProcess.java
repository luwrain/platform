// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.io.*;
import org.apache.logging.log4j.*;

import org.luwrain.core.*;

public final class BashProcess
{
    static private final Logger log = LogManager.getLogger();

    public enum Flags {ROOT, LOG_OUTPUT, LOG_ERRORS};

    public interface Listener
    {
	void onOutputLine(String line);
	void onErrorLine(String line);
	void onFinishing(int exitCode);
    }

    private final String command;
    private final String dir;
    private final Set<Flags> flags;
    private final Listener listener;
    private Process p = null;
    private int pid = -1;
    private int exitCode = -1;
    private final ArrayList<String> output = new ArrayList<>();
    private final ArrayList<String> errors = new ArrayList<>();
    private final AtomicBoolean done = new AtomicBoolean(false);
    private final AtomicBoolean doneOutput = new AtomicBoolean(false);
    private final AtomicBoolean doneErrors = new AtomicBoolean(false);

    public BashProcess(String command, String dir, Set<Flags> flags, Listener listener)
    {
	NullCheck.notEmpty(command, "command");
	NullCheck.notNull(flags, "flags");
	NullCheck.notNull(listener, "listener");
	this.command = command;
	this.dir = dir != null?dir:"/";
	this.flags = flags;
	this.listener = listener;
    }

    public BashProcess(String command, Set<Flags> flags, Listener listener)
    {
	this(command, null, flags, listener);
    }

    public BashProcess(String command, Set<Flags> flags)
    {
	this(command, flags, new EmptyListener());
    }

    public BashProcess(String command)
    {
	this(command, EnumSet.of(Flags.LOG_OUTPUT, Flags.LOG_ERRORS));
    }

    public void run() throws IOException
    {
	final String[] cmd = prepareCmd();
	log.debug("Running bash process: " + Arrays.toString(cmd));
	this.p = new ProcessBuilder(cmd).start();
	p.getOutputStream().close();
	final BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
	final String pidStr = r.readLine();
	if (pidStr == null)
	    throw new IOException("Bash process didn't return its pid");
	try {
	    this.pid = Integer.parseInt(pidStr);
	}
	catch(NumberFormatException e)
	{
	    log.error(e);
	    throw new IOException("'" + pidStr + "' is not a valid pid");
	}
	readOutput(r);
	readErrors(new BufferedReader(new InputStreamReader(p.getErrorStream())));
	new Thread(()->{
		try {
		    p.waitFor();
		    this.exitCode = p.exitValue();
		    synchronized(done){
			done.set(true);
			done.notifyAll();
		    }
		    listener.onFinishing(exitCode);
		}
		catch(InterruptedException e)
		{
		    Thread.currentThread().interrupt();
		}
	}).start();
    }

    public void stop()
    {
	try {
	    if (flags.contains(Flags.ROOT))
		new ProcessBuilder("sudo", "bash", "-c", "kill -KILL -" + String.valueOf(pid)).start(); else
	    new ProcessBuilder("bash", "-c", "kill -KILL -" + String.valueOf(pid)).start();
	}
	catch(IOException e)
	{
	    log.error(e);
	    throw new RuntimeException(e);
	}
    }

    public int waitFor()
    {
	try {
	    synchronized(done) {
		while(!done.get())
		    done.wait();
	    }
	    synchronized(doneOutput) {
		while (!doneOutput.get())
		    doneOutput.wait();
	    }
	    synchronized(doneErrors) {
		while (!doneErrors.get())
		    doneErrors.wait();
	    }
	    return exitCode;
	}
	catch(InterruptedException e)
	{
	    Thread.currentThread().interrupt();
	    return -1;
	}
    }

    private String[] prepareCmd()
    {
	if (flags.contains(Flags.ROOT))
	    return new String[]{
		"sudo",
		"setsid",
		"/bin/bash",
		"-c",
		"echo $$; cd " + escape(dir) + "; " + this.command
	    };
	return new String[]{
	    	    "setsid",
	    "/bin/bash",
	    "-c",
	    "echo $$; cd " + escape(dir) + "; " + this.command
	};
    }

    private void readOutput(BufferedReader r)
    {
	final boolean logOutput = flags.contains(Flags.LOG_OUTPUT);
	new Thread(()->{
		try {
		    try {
			String line = r.readLine();
			while (line != null)
			{
			    if (logOutput)
				output.add(line);
			    listener.onOutputLine(line);
			    line = r.readLine();
			}
		    }
		    finally {
			r.close();
			synchronized(doneOutput) {
			    doneOutput.set(true);
			    doneOutput.notifyAll();
			}
		    }
		}
		catch(IOException e)
		{
		    log.error("Unable to read the output of the bash process '" + command, e);
		}
	}).start();
    }

    private void readErrors(BufferedReader r)
    {
		final boolean logErrors = flags.contains(Flags.LOG_OUTPUT);
	new Thread(()->{
		try {
		    try {
			String line = r.readLine();
			while (line != null)
			{
			    if (logErrors)
				errors.add(line);
			    listener.onErrorLine(line);
			    line = r.readLine();
			}
		    }
		    finally {
			r.close();
			synchronized(doneErrors) {
			    doneErrors.set(true);
			    doneErrors.notifyAll();
			}
		    }
		}
		catch(IOException e)
		{
		    log.error("Unable to read the errors of the bash process '" + command + "': ");
		}
	}).start();
    }

        public String[] getOutput()
    {
	//FIXME: Checking the process finishes
	return output.toArray(new String[output.size()]);
    }

    public String[] getErrors()
    {
		//FIXME: Checking the process finishes
	return errors.toArray(new String[errors.size()]);
    }

    static public String escape(String value)
    {
	return "'" + value.replaceAll("'", "'\\\\''") + "'";
    }

    static public final class EmptyListener implements Listener
    {
	@Override public void onOutputLine(String line) {}
	@Override public void onErrorLine(String line) {}
	@Override public void onFinishing(int exitCode) {}
    }
}
