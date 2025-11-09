// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import org.luwrain.core.*;
import org.luwrain.core.events.InputEvent;
import org.luwrain.interaction.KeyboardHandler;

class KeyboardJavafxHandler implements KeyboardHandler
{
    private EventConsumer consumer;

    private boolean leftAltPressed = false;
    private boolean rightAltPressed = false;
    private boolean controlPressed = false;
    private boolean shiftPressed = false;

    @Override public void setEventConsumer(EventConsumer consumer)
    {
	this.consumer = consumer;
    }

    @Override public void onKeyPressed(Object obj)
    {
	final KeyEvent event=(KeyEvent)obj;
	if (consumer == null)
	    return;
	controlPressed=event.isControlDown();
	shiftPressed=event.isShiftDown();
	leftAltPressed=event.isAltDown();
	InputEvent.Special code = null;
	switch(event.getCode())
	{
	    // Functions keys
	case F1:
	    code=InputEvent.Special.F1;
	    break;
	case F2:
	    code=InputEvent.Special.F2;
	    break;
	case F3:
	    code=InputEvent.Special.F3;
	    break;
	case F4:
	    code=InputEvent.Special.F4;
	    break;
	case F5:
	    code=InputEvent.Special.F5;
	    break;
	case F6:
	    code=InputEvent.Special.F6;
	    break;
	case F7:
	    code=InputEvent.Special.F7;
	    break;
	case F8:
	    code=InputEvent.Special.F8;
	    break;
	case F9:
	    code=InputEvent.Special.F9;
	    break;
	case F10:
	    code=InputEvent.Special.F10;
	    break;
	case F11:
	    code=InputEvent.Special.F11;
	    break;
	case F12:
	    code=InputEvent.Special.F12;
	    break;
	case LEFT:
	    code=InputEvent.Special.ARROW_LEFT;
	    break;
	case RIGHT:
	    code=InputEvent.Special.ARROW_RIGHT;
	    break;
	case UP:
	    code=InputEvent.Special.ARROW_UP;
	    break;
	case DOWN:
	    code=InputEvent.Special.ARROW_DOWN;
	    break;
	case HOME:
	    code=InputEvent.Special.HOME;
	    break;
	case END:
	    code=InputEvent.Special.END;
	    break;
	case INSERT:
	    code=InputEvent.Special.INSERT;
	    break;
	case PAGE_DOWN:
	    code=InputEvent.Special.PAGE_DOWN;
	    break;
	case PAGE_UP:
	    code=InputEvent.Special.PAGE_UP;
	    break;
	case WINDOWS:
	    code=InputEvent.Special.WINDOWS;
	    break;
	case CONTEXT_MENU:
	    code=InputEvent.Special.CONTEXT_MENU;
	    break;
	case CONTROL:
	    code=InputEvent.Special.CONTROL;
	    break;
	case SHIFT:
	    code=InputEvent.Special.SHIFT;
	    break;
	case ALT:
	    code=InputEvent.Special.LEFT_ALT;
	    break;
	case ALT_GRAPH:
	    code=InputEvent.Special.RIGHT_ALT;
	    break;
	default:
	    return;
	}
	consumer.enqueueEvent(new InputEvent(code, shiftPressed, controlPressed, leftAltPressed));
    }

    @Override public void onKeyReleased(Object obj)
    {
	final KeyEvent event = (KeyEvent)obj;
	if (consumer == null)
	    return;
	controlPressed=event.isControlDown();
	shiftPressed=event.isShiftDown();
	leftAltPressed=event.isAltDown();
    }

    @Override public void onKeyTyped(Object obj)
    {
	final KeyEvent event = (KeyEvent)obj;
	if (consumer == null)
	    return;
	controlPressed=event.isControlDown();
	shiftPressed=event.isShiftDown();
	leftAltPressed=event.isAltDown();
	final String keychar=event.getCharacter();
	InputEvent.Special code = null;
	if(keychar.equals("\b"))
	    code=InputEvent.Special.BACKSPACE; else
	    if(keychar.equals("\n")||keychar.equals("\r")) 
		code=InputEvent.Special.ENTER; else 
		if(keychar.equals("\u001b")) 
		    code=InputEvent.Special.ESCAPE; else
		    if(keychar.equals("\u007f"))
			code=InputEvent.Special.DELETE; else
			if(keychar.equals("\t")) 
			    code=InputEvent.Special.TAB; else
			{
			    // FIXME: javafx characters return as String type we need a char (now return first symbol)
			    char c = event.getCharacter().charAt(0);
			    final InputEvent emulated=new InputEvent(c, shiftPressed,controlPressed,leftAltPressed);
			    consumer.enqueueEvent(emulated);
			    return;
			}
	//	final int _code=code;
	consumer.enqueueEvent(new InputEvent(code, 
						shiftPressed,controlPressed,leftAltPressed));
    }
}
