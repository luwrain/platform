// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import org.luwrain.core.*;

public final class SysDevice
{
    public enum Type {
	UNKNOWN,
	PCI,
	USB,
    };

    public final Type type;
    public final String id;
    public final String cls;
    public final String vendor;
    public final String model;
    public final String driver;
    public final String module;

    SysDevice(Type type,
		  String id,
		  String cls,
		  String vendor,
		  String model,
		  String driver,
		  String module)
    {
	NullCheck.notNull(type, "type");
	NullCheck.notNull(id, "id");
	NullCheck.notNull(cls, "cls");
	NullCheck.notNull(vendor, "vendor");
	NullCheck.notNull(model, "model");
	NullCheck.notNull(driver, "driver");
	NullCheck.notNull(module, "module");
	this.type = type;
	this.id = id;
	this.cls = cls;
	this.vendor = vendor;
	this.model = model;
	this.driver = driver;
	this.module = module;
    }
}
