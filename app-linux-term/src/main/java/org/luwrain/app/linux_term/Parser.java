
package org.luwrain.app.linux_term;

import java.util.*;
import java.util.stream.*;
import org.apache.logging.log4j.*;

import static java.util.Objects.*;

final class Parser
{
    static private final Logger log = LogManager.getLogger();

        // Automaton states
    private enum State {
        NORMAL,       // Regular text
        ESCAPE,       // Received \033
        CSI_PARAM     // After \033[ (Control Sequence Introducer)
    }

    /**
     * Parses the terminal output to extract commands.
     */
    public List<Output> parse(String input)
    {
        final List<Output> commands = new ArrayList<>();
        State state = State.NORMAL;
        StringBuilder rawBuffer = new StringBuilder();
        StringBuilder paramBuffer = new StringBuilder();
        String privateMarker = "";
        for (int i = 0; i < input.length(); i++)
	{
            final char c = input.charAt(i);
	    //log.trace("Parsing '{}'", c);
            switch (state)
	    {
                case NORMAL:
		    // ESCAPE
                    if (c == '\033')  
		    {
                        state = State.ESCAPE;
                        rawBuffer.setLength(0);
                        rawBuffer.append(c);
                    } else
			commands.add(new OutputText(String.valueOf(c)));
                    break;

                case ESCAPE:
                    rawBuffer.append(c);
                    if (c == '[')
		    {
                        state = State.CSI_PARAM;
                        paramBuffer.setLength(0);
                        privateMarker = "";
                    } else
			if (c == ']')
			{
                        // OSC (Operating System Command) - например, смена заголовка окна.
                        // Для упрощения примера сбрасываем, но в реальном TUI здесь нужен свой State.
                        state = State.NORMAL; 
                    } else
			{
                        // Одиночные ESC-команды (например ESC M - Reverse Index)
                        state = State.NORMAL;
                    }
                    break;

                case CSI_PARAM:
                    rawBuffer.append(c);
                    // Приватные маркеры (например, ? для режимов DEC)
                    if (c >= 0x3C && c <= 0x3F)
                        privateMarker = String.valueOf(c); else
                    // Числа и разделитель (точка с запятой)
			if ((c >= '0' && c <= '9') || c == ';')
                        paramBuffer.append(c); else
                    // Финальный символ (определяет тип команды, буквы от @ до ~)
			if (c >= 0x40 && c <= 0x7E)
			{
                        List<Integer> params = parseParameters(paramBuffer.toString());
                        String desc = resolveCommandDescription(c, privateMarker, params);
                        commands.add(new AnsiCommand(
                                rawBuffer.toString(), c, privateMarker, params, desc
                        ));
                        
                        state = State.NORMAL; // Возвращаемся к тексту
                    }
                    // Иначе (например пробел) - просто промежуточные символы, в базовом CSI игнорим
                    break;
            }
        }
        return compacting(commands);
    }

    /**
     * Парсит строку вида "31;1;;4" в список чисел.
     * Пропущенные значения неявно считаются нулями (стандартное поведение xterm).
     */
    private List<Integer> parseParameters(String paramStr) {
        if (paramStr.isEmpty())
	{
            return new ArrayList<>(); // Без аргументов
        }
        return Arrays.stream(paramStr.split(";", -1))
                .map(s -> s.isEmpty() ? 0 : Integer.parseInt(s))
                .collect(Collectors.toList());
    }

    /**
     * Translates command codes to the human readable form.
     */
    private String resolveCommandDescription(char finalChar, String marker, List<Integer> params)
    {
        if ("?".equals(marker))
	{
            switch (finalChar)
	    {
                case 'h':
		    return "DEC SET (Включить режим)";
                case 'l':
		    return "DEC RESET (Выключить режим)";
                default:
		    return "DEC Private Command";
            }
        }
        switch (finalChar)
	{
            case 'A':
		return "CursorUp";
            case 'B':
		return "CursorDown";
            case 'C':
		return "CursorForward";
            case 'D':
		return "CursorBack";
            case 'H': 
            case 'f':
		return "CursorPosition";
            case 'J':
		return "EraseInDisplay";
            case 'K':
		return "EraseInLine";
            case 'm': 
                // m - самая популярная команда (цвета, жирность)
                return "TextColor";
            case 's':
		return "SaveCursor";
            case 'u':
		return "RestoreCursorPos";
            default:
		return "Unknown/Other Control Sequence";
        }
    }

    static List<Output> compacting(List<Output> output)
    {
	final var res = new ArrayList<Output>();
	var b = new StringBuilder();
	for(var o: output)
	{
	    if (o instanceof AnsiCommand)
	    {
		if (b.length() > 0)
		{
		    res.add(new OutputText(new String(b)));
		    b = new StringBuilder();
		}
		res.add(o);
	    }
	    if (o instanceof OutputText text)
		b.append(text.text);
	}
	if (b.length() > 0)
	    res.add(new OutputText(new String(b)));
	return res;
    }

    static class Output
    {
    }

    static final class OutputText extends Output
    {
	final String text;

	OutputText(String text)
	{
	    this.text = text;
	}

	@Override public String toString()
	{
	    return text;
	}
    }

            static final class AnsiCommand extends Output
    {
        final String rawSequence;
        final char finalChar;
        final String privateMarker;
        final List<Integer> params;
        final String description;

        AnsiCommand(String rawSequence, char finalChar, String privateMarker, List<Integer> params, String description)
	{
            this.rawSequence = rawSequence;
            this.finalChar = finalChar;
            this.privateMarker = privateMarker;
            this.params = requireNonNullElse(params, new ArrayList<>());
            this.description = description;
        }

        @Override public String toString()
	{
            return String.format("ANSI: %-30s, marker: '%s', params: %-10s, final: '%c', raw: %s",
                    description, privateMarker, params, finalChar, 
                    rawSequence.replace("\033", "\\e")); // заменяем непечатный ESC для вывода
        }
    }

    
}
