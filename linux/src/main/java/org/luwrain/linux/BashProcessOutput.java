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

    @Override public void onOutputLine(String line)
    {
	if (skipEmpty && line.isEmpty())
	    return;
	output.add(line);
    }
    
    @Override public void onErrorLine(String line)
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

        public List<String> getErrors()
    {
	return Collections.unmodifiableList(errors);
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
