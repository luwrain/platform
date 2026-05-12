// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import org.luwrain.core.*;
import static java.util.Objects.*;

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
	requireNonNull(type, "type can't be null");
	requireNonNull(id, "id can't be null");
	requireNonNull(cls, "cls can't be null");
	requireNonNull(vendor, "vendor can't be null");
	requireNonNull(model, "model can't be null");
	requireNonNull(driver, "driver can't be null");
	requireNonNull(module, "module can't be null");
	this.type = type;
	this.id = id;
	this.cls = cls;
	this.vendor = vendor;
	this.model = model;
	this.driver = driver;
	this.module = module;
    }
}
