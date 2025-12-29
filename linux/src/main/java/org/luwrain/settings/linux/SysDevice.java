/*
   Copyright 2012-2018 Michael Pozhidaev <michael.pozhidaev@gmail.com>

   This file is part of LUWRAIN.

   LUWRAIN is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public
   License as published by the Free Software Foundation; either
   version 3 of the License, or (at your option) any later version.

   LUWRAIN is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   General Public License for more details.
*/

package org.luwrain.settings.linux;

import org.luwrain.core.*;
import org.luwrain.core.events.*;
import org.luwrain.controls.*;
import org.luwrain.cpanel.*;
import org.luwrain.linux.*;

class SysDevice extends SimpleArea implements SectionArea
{
    private final ControlPanel controlPanel;
    private final Luwrain luwrain;
    private final org.luwrain.linux.SysDevice device;

    SysDevice(ControlPanel controlPanel, org.luwrain.cpanel.Element el)
    {
	super(new DefaultControlContext(controlPanel.getCoreInterface()), "Информация об устройстве");
	NullCheck.notNull(controlPanel, "controlPanel");
	NullCheck.notNull(el, "el");
	this.controlPanel = controlPanel;
	this.luwrain = controlPanel.getCoreInterface();
	if (!(el instanceof org.luwrain.settings.linux.SysDevice.Element ))
	    throw new IllegalArgumentException("el must be an instance of org.luwrain.settings.HardwareSysDevice.Element ");
	final org.luwrain.settings.linux.SysDevice.Element sysEl = (org.luwrain.settings.linux.SysDevice.Element )el;
	device = sysEl.device;
	fillData();
    }

    private void fillData()
    {
	update((lines)->{
		lines.add("Тип: " + device.type);
		lines.add("Класс: " + device.cls);
		lines.add("Идентификатор: " + device.id);
		lines.add("Производитель: " + device.vendor);
		lines.add("Модель: " + device.model);
		lines.add("Драйвер: " + device.driver);
		lines.add("Модуль: " + device.module);
		lines.add("");
	    });
    }

    @Override public boolean onInputEvent(InputEvent event)
    {
	NullCheck.notNull(event, "event");
	if (controlPanel.onInputEvent(event))
	    return true;
	return super.onInputEvent(event);
    }

    @Override public boolean onSystemEvent(SystemEvent event)
    {
	NullCheck.notNull(event, "event");
	if (controlPanel.onSystemEvent(event))
	    return true;
	return super.onSystemEvent(event);
    }

    @Override public boolean saveSectionData()
    {
	return true;
    }

    static SysDevice create(ControlPanel controlPanel, org.luwrain.cpanel.Element el)
    {
	NullCheck.notNull(controlPanel, "controlPanel");
	NullCheck.notNull(el, "el");
	return new SysDevice(controlPanel, el);
    }

    static final class Element implements org.luwrain.cpanel.Element
    {
	private final org.luwrain.cpanel.Element parent;
	private final org.luwrain.linux.SysDevice device;

	Element(org.luwrain.cpanel.Element parent, org.luwrain.linux.SysDevice device)
	{
	    NullCheck.notNull(parent, "parent");
	    NullCheck.notNull(device, "device");
	    this.parent = parent;
	    this.device = device;
	}

	@Override public org.luwrain.cpanel.Element getParentElement()
	{
	    return parent;
	}

	@Override public boolean equals(Object o)
	{
	    if (o == null || !(o instanceof org.luwrain.settings.linux.SysDevice.Element))
		return false;
	    final org.luwrain.settings.linux.SysDevice.Element el = (org.luwrain.settings.linux.SysDevice.Element)o;
	    return device.id.equals(el.device.id);
	}

	@Override public String toString()
	{
	    return device.cls + " " + device.vendor + " " + device.model;
	}
    }
}
