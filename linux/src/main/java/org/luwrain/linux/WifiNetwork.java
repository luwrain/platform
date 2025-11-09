// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

public interface WifiNetwork
{
    String getName();
    String getProtectionType();
    int getSignalLevel();
    boolean isConnected();
}
