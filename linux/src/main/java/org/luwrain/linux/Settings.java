// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import org.luwrain.core.*;
import org.luwrain.util.*;

public interface Settings
{
    static public final String NETWORK_PATH = "/org/luwrain/network";
    static public final String NETWORKS_PATH = "/org/luwrain/network/wifi-networks";

public interface Network
{
    String getDefaultWifiNetwork(String defValue);
    void setDefaultWifiNetwork(String value);
}

    public interface WifiNetwork
    {
	String getPassword(String defValue);
	void setPassword(String value);
    }

    static public Network createNetwork(Registry registry)
    {
	NullCheck.notNull(registry, "registry");
	return RegistryProxy.create(registry, NETWORK_PATH, Network.class);
    }

    static String makeRegistryName(String value)
    {
	return value.replaceAll("/", "_").replaceAll("\n", "_").replaceAll(" ", "_");
    }
}
