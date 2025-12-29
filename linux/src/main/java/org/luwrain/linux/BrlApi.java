// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import java.util.concurrent.*;

import org.luwrain.core.*;
import org.luwrain.core.events.*;

public class BrlApi implements Braille
{
    @Override public InitResult init(EventConsumer eventConsumer)
    {
	return new InitResult();
	    }

    @Override synchronized public void writeText(String text)
    {
    }

    @Override public String getDriverName()
    {
	return "";
    }

    @Override public int getDisplayWidth()
    {
	return 0;
    }

    @Override public int getDisplayHeight()
    {
	return 0;
    }

    synchronized private void readKeys()
    {
    }
}
