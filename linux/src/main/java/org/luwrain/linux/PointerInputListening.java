// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import java.io.*;
import java.util.concurrent.*;
import org.luwrain.core.events.*;

import org.luwrain.core.*;

class PointerInputListening
{
    static private final String LOG_COMPONENT = Linux.LOG_COMPONENT;
    
    static private final long DOUBLE_CLICK_DELAY_MSEC = 500;
    static private final int STEP_X = 30;
    static private final int STEP_Y = 30;

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Luwrain luwrain;
    private final String fileName;
    private FutureTask task = null;

    private int posX = 0;
    private int posY = 0;
    private long prevTimeMsec = -1;

    PointerInputListening(Luwrain luwrain, String fileName)
    {
	NullCheck.notNull(luwrain, "luwrain");
	NullCheck.notEmpty(fileName, "fileName");
	this.luwrain = luwrain;
	this.fileName = fileName;
    }

    void run()
    {
	task = createTask();
	Log.debug(LOG_COMPONENT, "starting pointer input listening on " + fileName);
	executor.execute(task);
    }

    private FutureTask createTask()
    {
	return new FutureTask<>(()->{
		DataInputStream s = null;
		try {
		    s = new DataInputStream(new FileInputStream("/dev/input/mice"));
		    do {
			final int code = s.readUnsignedByte();
			final int x = s.readByte();
			final int y = s.readByte();
			if ((code & 1) > 0)
			{
			    final long currentMsec = System.currentTimeMillis();
			    if (prevTimeMsec >= 0 && (currentMsec - prevTimeMsec <= DOUBLE_CLICK_DELAY_MSEC))
				luwrain.sendInputEvent(new InputEvent(InputEvent.Special.ENTER));
			    prevTimeMsec = currentMsec;

			}
			if ((code & 2) > 0)
			    luwrain.sendInputEvent(new InputEvent(InputEvent.Special.CONTEXT_MENU));
			if ((code & 8) > 0)
			    onOffset(x, y);
		    } while(true);
		}
		catch(Exception e)
		{
		    try {
			if (s != null)
			    s.close();
		    }
		    catch(IOException ee)
		    {
		    }
		    Log.error(LOG_COMPONENT, "unable to get pointer input events:" + e.getClass().getName() + ":" + e.getMessage());
		}
	}, null);
    }

    private void onOffset(int x, int y)
    {
	posX += x;
	posY += y;
	boolean step = false;
	do {
	    step = false;
	    while (posX > STEP_X)
	    {
		luwrain.sendInputEvent(new InputEvent(InputEvent.Special.ARROW_RIGHT));
		posX -= STEP_X;
		step = true;
	    }
	    while (posY > STEP_Y)
	    {
		luwrain.sendInputEvent(new InputEvent(InputEvent.Special.ARROW_UP));
		posY -= STEP_Y;
		step = true;
	    }
	    while (posX < -1 * STEP_X)
	    {
		luwrain.sendInputEvent(new InputEvent(InputEvent.Special.ARROW_LEFT));
		posX += STEP_X;
		step = true;
	    }
	    while (posY < -1 * STEP_Y)
	    {
		luwrain.sendInputEvent(new InputEvent(InputEvent.Special.ARROW_DOWN));
		posY += STEP_Y;
		step = true;
	    }
	} while(step);
    }
}
