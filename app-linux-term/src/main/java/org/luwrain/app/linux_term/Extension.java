
package org.luwrain.app.linux_term;

import com.google.auto.service.*;
import org.luwrain.core.*;

@AutoService(org.luwrain.core.Extension.class)
public final class Extension extends EmptyExtension
{
    @Override public Command[] getCommands(Luwrain luwrain)
    {
	return new Command[]{ new SimpleShortcutCommand("term") };
    }

    @Override public ExtensionObject[] getExtObjects(Luwrain luwrain)
    {
	return new ExtensionObject[]{
	    new DefaultShortcut("term", App.class){
		@Override public Application[] prepareApp(String[] args)
		{
		    if (args.length == 1 && !args[0].isEmpty())
			return new Application[]{new App(new TermInfo(), args[0])};
		    final String dir = luwrain.getActiveAreaAttr(Luwrain.AreaAttr.DIRECTORY);
		    if (dir != null && !dir.isEmpty())
			return new Application[]{new App(new TermInfo(), dir)};
		    return new Application[]{new App(new TermInfo())};
		}
	    }
	};
    }
}
