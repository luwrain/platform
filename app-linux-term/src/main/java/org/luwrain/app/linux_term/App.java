// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.app.linux_term;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import org.apache.logging.log4j.*;

import com.pty4j.*;
import com.pty4j.unix.*;

import org.luwrain.core.*;
import org.luwrain.app.base.*;

import static java.util.Objects.*;

public final class App extends AppBase<Strings>
{
    static private final Logger log = LogManager.getLogger();
    static private final long LISTENING_DELAY = 10;

    final TermInfo termInfo;
    final String startingDir;
    private UnixPtyProcess  pty = null;
    private MainLayout layout = null;

    private volatile StringBuilder termOutput = new StringBuilder();
    private volatile long latestOutputTimestamp = new Date().getTime();

    public App(TermInfo termInfo)
    {
	super(Strings.class, "luwrain.linux.term");
	this.termInfo = requireNonNull(termInfo, "termInfo can't be null");
	this.startingDir = null;
    }

    public App(TermInfo termInfo, String startingDir)
    {
	super(Strings.class, "luwrain.linux.term");
	this.termInfo = requireNonNull(termInfo, "termInfo can't be null");
	this.startingDir = startingDir;
    }

    @Override public AreaLayout onAppInit() throws IOException
    {
	final Map<String, String> env = new HashMap<>(System.getenv());
	env.put("TERM", "linux");
	this.pty = (UnixPtyProcess)(new PtyProcessBuilder(new String[]{"/bin/bash", "-l"})
				    .setEnvironment(env)
				    .setDirectory((this.startingDir != null && !startingDir.isEmpty())?startingDir:getLuwrain().getProperty("luwrain.dir.userhome"))
				    .setConsole(false)
				    .start());
	getLuwrain().executeBkg(new FutureTask<>(()->readOutput(), null));
		getLuwrain().executeBkg(new FutureTask<>(()->listening(), null));
	setAppName(getStrings().appName());
	this.layout = new MainLayout(this);
	return layout.getLayout();
    }

    private void readOutput()
    {
	try {
	    try {
		final InputStream is = pty.getInputStream();
		final InputStreamReader r = new InputStreamReader(is, "UTF-8");
		while(pty.isRunning())
		{
			final int c = r.read();
			if (c < 0)
			{
			    log.debug("Negative character from the terminal: " + c);
			    break;
			}
					    synchronized(App.this) {
			this.termOutput.append((char)c);
			this.latestOutputTimestamp = new Date().getTime();
		    }
		    		}
		log.debug("Closing the terminal");
		//FIXME: Read complete output
		r.close();
		is.close();
		try {
		    pty.waitFor();
		}
		catch(InterruptedException e)
		{
		    Thread.currentThread().interrupt();
		}
		log.debug("Exit value is " + pty.exitValue());
	    }
	    catch(Exception e)
	    {
		crash(e);
	    }
	}
	catch(Throwable t)
	{
	    log.error("PPTY failure", t);
	}
    }

    private void listening()
    {
	try {
	    while(pty.isRunning())
	    {
		try {
		    Thread.sleep(LISTENING_DELAY);
		}
		catch(InterruptedException e)
		{
		    Thread.currentThread().interrupt();
		    return;
		}
		final String output;
		synchronized(App.this) { 
		    if (this.termOutput.length() == 0)
			continue;
		    final long timestamp = new Date().getTime();
		    if (timestamp - latestOutputTimestamp < LISTENING_DELAY)
			continue;
		    output = new String(this.termOutput);
		    this.termOutput = new StringBuilder();
		}
		getLuwrain().runUiSafely(()->{
			if (layout != null)
			    layout.termText(output);
		    });
	    }
	}
	finally {
	    log.debug("Finishing listening thread, running=" + pty.isRunning());
	    synchronized(App.this) {
		final String output = new String(this.termOutput);
		this.termOutput = new StringBuilder();
		if (!output.isEmpty())
		    getLuwrain().runUiSafely(()->{
			    if (layout != null)
				layout.termText(output);
			});
	    };
	}
    }

void sendChar(char ch)
    {
	try {
	    	if (ch < 32)
	{
	    pty.getOutputStream().write((byte)ch);
	    return;
	}
	    final OutputStreamWriter w = new OutputStreamWriter(pty.getOutputStream(), "UTF-8");
	    w.write(ch);
	    w.flush();
	    	}
	catch(IOException e)
	{
	    getLuwrain().crash(e);
	}
	    }

    @Override public void closeApp()
    {
	pty.hangup();
	pty.destroy();
	super.closeApp();
    }
}
