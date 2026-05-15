// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2026 Michael Pozhidaev <msp@luwrain.org>

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

    final String startingDir;
    private UnixPtyProcess  pty;
    private MainLayout layout;

    private volatile StringBuilder termOutput = new StringBuilder();
    private volatile long latestOutputTimestamp = new Date().getTime();

    public App()
    {
	super(Strings.class, "luwrain.linux.term");
	this.startingDir = null;
    }

    public App(String startingDir)
    {
	super(Strings.class, "luwrain.linux.term");
	this.startingDir = startingDir;
    }

    @Override public AreaLayout onAppInit() throws IOException
    {
	final Map<String, String> env = new HashMap<>(System.getenv());
	env.put("TERM", "linux");
	this.pty = (UnixPtyProcess)(new PtyProcessBuilder(new String[]{"/bin/bash", "-l"})
				    .setEnvironment(env)
				    .setDirectory((this.startingDir != null && !startingDir.isEmpty())?startingDir:getLuwrain().getPath("~"))
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
	    try (final InputStream is = pty.getInputStream()) {
		try (final InputStreamReader r = new InputStreamReader(is, "UTF-8")) {
		    while(pty.isRunning())
		    {
			final int c = r.read();
			log.trace("Input {} ({})", (char)c, c);
			if (c < 0)
			{
			    log.trace("Negative character from the terminal: {}", c);
			    break;
			}
			synchronized(App.this) {
			    this.termOutput.append((char)c);
			    this.latestOutputTimestamp = new Date().getTime();
			}
		    }
		    log.debug("Closing the terminal");
		    //FIXME: Read complete output
		}
	    }
	    try {
		pty.waitFor();
	    }
	    catch(InterruptedException e)
	    {
		Thread.currentThread().interrupt();
	    }
	    log.debug("Exit value is {}", pty.exitValue());
	}
	catch(Throwable e)
	{
	    log.error("PPTY failure", e);
	    crash(e);
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
	    log.trace("Finishing listening thread, running=" + pty.isRunning());
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

void sendChar(int ch)
    {
	try {
	    	if (ch < 128)
	{
	    pty.getOutputStream().write(ch);
	    pty.getOutputStream().flush();
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

    void sendChar(byte[] ch)
    {
	try {
	    pty.getOutputStream().write(ch);
	    pty.getOutputStream().flush();
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
