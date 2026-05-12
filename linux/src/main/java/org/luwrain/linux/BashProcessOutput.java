// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import java.util.*;

public class BashProcessOutput implements BashProcess.Listener
{
    protected final List<String>
	output = new ArrayList<>(),
	errors = new ArrayList<>();

    protected final boolean errToOutput, skipEmpty;
    protected int exitCode = -1;

    public BashProcessOutput(boolean errToOutput, boolean skipEmpty)
    {
	this.errToOutput = errToOutput;
	this.skipEmpty = skipEmpty;
    }

    public BashProcessOutput(boolean errToOutput)
    {
	this(errToOutput, false);
    }

    public BashProcessOutput()
    {
	this(false, false);
    }

    @Override public synchronized void onOutputLine(String line)
    {
	if (skipEmpty && line.isEmpty())
	    return;
	output.add(line);
    }
    
    @Override public synchronized void onErrorLine(String line)
    {
		if (skipEmpty && line.isEmpty())
	    return;
		if (errToOutput)
		    output.add(line); else
		    errors.add(line);
    }
    
    @Override public void onFinishing(int exitCode)
    {
	this.exitCode = exitCode;
    }

    public List<String> getOutput()
    {
	return Collections.unmodifiableList(output);
    }

    public synchronized String[] getOutputAsArray()
    {
	return output.toArray(new String[output.size()]);
    }

        public List<String> getErrors()
    {
		return Collections.unmodifiableList(errors);
    }

    public synchronized String[] getErrorsAsArray()
    {
		return errors.toArray(new String[errors.size()]);
    }

    public boolean isFinished()
    {
	return exitCode >= 0;
    }

    public int getExitCode()
    {
	return exitCode;
    }
}
