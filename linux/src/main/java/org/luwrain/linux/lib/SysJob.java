// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux.lib;

import java.util.*;
import java.io.*;

import com.google.auto.service.*;

import org.luwrain.core.*;
import org.luwrain.linux.*;

import static java.util.Objects.*;
import static java.util.stream.Collectors.*;
import static org.luwrain.core.NullCheck.*;

@AutoService(JobLauncher.class)
public final class SysJob implements JobLauncher
{

    @Override public Job launch(Job.Listener listener, String[] args, String dir)
    {
	requireNonNull(listener, "listener can't be null");
	notNullItems(args, "args");
	if (args.length == 0 || args[0].isEmpty())
	    return new ErrorJobInstance("sys", "No command");
	final Data data = new Data();
	final Job ins = new Job(){
		@Override public void stop() { if (data.stopProc != null) data.stopProc.run(); }
	    	@Override public String getInstanceName() { return data.cmd; }
		@Override public Status getStatus() { return data.finished?Status.FINISHED:Status.RUNNING; }
		@Override public int getExitCode() { return data.exitCode; }
		@Override public boolean isFinishedSuccessfully() { return data.finished && data.exitCode == 0; }
		@Override public List<String> getInfo(String type)
		{
		    requireNonNull(type, "type can't be null");
		    switch(type)
		    {
		    case "brief":
			return Arrays.asList(data.state);
		    case "main":
			return data.mlState;
		    default:
			return Arrays.asList();
		    }
		}
			    };
	data.cmd = buildCmd(args);
	final var p = new BashProcess(data.cmd, dir, EnumSet.noneOf(BashProcess.Flags.class), new BashProcess.Listener(){
		@Override public void onOutputLine(String line)
		{
		    data.mlState.add(line);
		    listener.onInfoChange(ins, "main", data.mlState);
		}
		@Override public void onErrorLine(String line)
		{
		    data.mlState.add(line);
		    listener.onInfoChange(ins, "main", data.mlState);
		}
		@Override public void onFinishing(int exitCode)
		{
		    data.finished = true;
		    data.exitCode = exitCode;
		    listener.onStatusChange(ins);
		}
	    });
	try {
	    p.run();
	}
	catch(IOException e)
	{
	    return new ErrorJobInstance(args[0], e.getMessage());
	}
	data.stopProc = ()->p.stop();
	return ins;
    }

    @Override public String getExtObjName()
    {
	return "sys";
    }

    @Override public Set<Flags> getJobFlags()
    {
	return EnumSet.noneOf(Flags.class);
    }

    static private String buildCmd(String[] args)
    {
	NullCheck.notNullItems(args, "args");
	if (args.length == 0)
	    throw new IllegalArgumentException("args can't be empty");
	if (args[0].isEmpty())
	    throw new IllegalArgumentException("args[0] can't be empty");
	return Arrays.asList(args).stream().collect(joining(" "));
    }

    static private final class Data
    {
	String cmd;
	boolean finished = false;
	int exitCode = -1;
	String state = "";
	final List<String> mlState = new ArrayList<>();
	Runnable stopProc = null;
    }
}
