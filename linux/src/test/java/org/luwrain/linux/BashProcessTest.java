/*
   Copyright 2012-2024 Michael Pozhidaev <msp@luwrain.org>

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

package org.luwrain.linux;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class BashProcessTest
{
    @Test public void escapeEmpty()
    {
	assertEquals("''", BashProcess.escape(""));
    }

    @Test public void escapeSimple()
    {
	assertEquals("'abc'", BashProcess.escape("abc"));
    }

    @Test public void escapeComplex()
    {
	assertEquals("'a'\\''b'\\''c'", BashProcess.escape("a'b'c"));
    }

    @Test public void escapeComplex2()
    {
	assertEquals("'Michael Learns To Rock - That'\\''s Why You Go Away [Official Video] (with Lyrics Closed Caption)-4T5g-9E6PUs.mkv'", BashProcess.escape("Michael Learns To Rock - That's Why You Go Away [Official Video] (with Lyrics Closed Caption)-4T5g-9E6PUs.mkv"));
    }

    @Test public void output() throws Exception
    {
	final BashProcessOutput output = new BashProcessOutput();
	final BashProcess b = new BashProcess("echo proba", output);
	b.run();
	assertEquals(0, b.waitFor());
	final String[] res = output.getOutputAsArray();
	assertNotNull(res);
	assertEquals(1, res.length);
	assertEquals("proba", res[0]);
	
    }

    
}
