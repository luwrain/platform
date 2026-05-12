// SPDX-License-Identifier: BUSL-1.1
// Copyright 2012-2025 Michael Pozhidaev <msp@luwrain.org>

package org.luwrain.linux;

import java.util.*;
import java.io.*;
import java.nio.charset.*;

import org.luwrain.core.NullCheck;

import static java.util.Objects.*;

public class ConsoleReader
{
    protected List<Item> items = new LinkedList<Item>();
    protected StringBuilder newText = new StringBuilder();

    public void read(Reader reader) throws IOException
    {
	requireNonNull(reader, "reader can't be null");
	while (reader.ready())
	{
	    final int c = reader.read();
	    if (c == 27)//special escape sequence
	    {
		commit();
		onEscapeSeq(reader);
		continue;
	    }
	    if (Character.isISOControl(c))
	    {
		commit();
		onIsoControl(c); 
		continue;
	    }
	    onRegularChar((char)c);
	}
    }

    protected void onRegularChar(char c)
    {
	newText.append(c);
    }

    protected void onIsoControl(int c)
    {
	switch(c)
	{
	case 7://bell
	    items.add(new Item(Item.Type.BELL));
	    return;
	case 10://new line
	    items.add(new Item(Item.Type.NEW_LINE));
	    return;
	case '\b':
	    items.add(new Item(Item.Type.BACKSPACE));
	    return;
	}
    }

    protected void onEscapeSeq(Reader reader) throws IOException
    {
	requireNonNull(reader, "reader can't be null");
	int c = reader.read();
	if (c < 0)
	    return;
	if (c == '[')
	{
	    c = reader.read();
	    if (c < 0)
		return;
	    if (c == 'C')
	    {
		items.add(new Item(Item.Type.KEY_RIGHT));
		return;
	    } //arrow right
	}
    }

    public void commit()
    {
	final String text = new String(newText);
	if (!text.isEmpty())
	    items.add(new Item(text));
	newText = new StringBuilder();
    }

    public void clear()
    {
	newText = new StringBuilder();
	items.clear();
    }

    public Item[] getItems()
    {
	return items.toArray(new Item[items.size()]);
    }

    static public class Item 
    {
	public enum Type 
	{
	    TEXT,
	    BELL,
	    NEW_LINE,
	    BACKSPACE,
	    KEY_RIGHT,
	};

	public final Type type;
	public final String text;

	public Item(Type type)
	{
	    requireNonNull(type, "type can't be null");
	    this.type = type;
	    this.text = null;
	}

	public Item(String text)
	{
	    requireNonNull(text, "text can't be null");
	    this.type = Type.TEXT;
	    this.text = text;
	}

	@Override public String toString()
	{
	    if (type == Type.TEXT && text != null)
		return "[" + type.toString() + "] " + text;
	    return "[" + type.toString() + "]";
	}
    }
}
