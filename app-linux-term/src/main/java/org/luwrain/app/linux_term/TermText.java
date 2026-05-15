
package org.luwrain.app.linux_term;

import java.util.ArrayList;
import java.util.List;

import org.luwrain.core.*;

/**
 * Text content of ANSI terminal emulation.
 * Lines are stored in a list (one line — one StringBuilder).
 * Cursor position is 0-based (row, column).
 * Parameters originRow/originCol control the virtual origin in origin mode.
 */
public class TermText implements Lines, HotPoint
{
    // Terminal line buffer
    private final List<StringBuilder> buffer;
    // Screen dimensions
    private final int rows, cols;

    // Current cursor position (0-based)
    private int cursorRow, cursorCol;

    // Position of the top-left corner (origin in origin mode)
    private int originRow, originCol;
    private boolean originMode;

    // Scroll region (topMargin, bottomMargin inclusive, 0-based)
    private int topMargin, bottomMargin;

    /**
     * Creates a terminal text buffer of the given size.
     * @param rows number of rows
     * @param cols number of columns
     */
    public TermText(int rows, int cols)
    {
        this.rows = rows;
        this.cols = cols;
        this.buffer = new ArrayList<>(rows);
        for (int i = 0; i < rows; i++) 
            buffer.add(new StringBuilder());
        this.cursorRow = 1;
        this.cursorCol = 1;
        this.originRow = 1;
        this.originCol = 1;
        this.originMode = false;
        this.topMargin = 1;
        this.bottomMargin = rows;
    }

    // Cursor positioning commands
    /**
     * Analog of CUP (Cursor Position).
     * Sets the cursor to the specified position.
     * If originMode is on, coordinates are relative to (originRow, originCol).
     * @param row row number (0..rows-1)
     * @param col column number (0..cols-1)
     */
    void setCursorPos(int row, int col)
    {
        if (row < 0)
	    row = 0;
        if (col < 0)
	    col = 0;
        final int targetRow, targetCol;
        if (originMode)
	{
            targetRow = originRow + row;
            targetCol = originCol + col;
        } else
	{
            targetRow = row;
            targetCol = col;
        }
        // Clamp to screen boundaries
        cursorRow = clamp(targetRow, 1, rows);
        // Column is allowed to be set to cols+1? According to the standard it is,
        // but for simplicity we limit to screen width.
        cursorCol = clamp(targetCol, 0, cols - 1);
    }

    /**
     * Moves cursor to home position.
     * In originMode — to (originRow, originCol), otherwise to (0, 0).
     */
    public void cursorHome()
    {
        if (originMode) 
            setCursorPos(0, 0); else // recalculates via origin
{
            cursorRow = 0;
            cursorCol = 0;
        }
    }

    /** Move up by n lines. */
    public void cursorUp(int n) {
        int maxUp = (originMode ? topMargin : 0);
        cursorRow = Math.max(cursorRow - n, maxUp);
    }

    /** Move down by n lines. */
    public void cursorDown(int n) {
        int maxDown = (originMode ? bottomMargin : rows);
        cursorRow = Math.min(cursorRow + n, maxDown - 1);
    }

    /** Move right by n columns. */
    public void cursorRight(int n) {
        cursorCol = Math.min(cursorCol + n, cols - 1);
    }

    /** Move left by n columns. */
    public void cursorLeft(int n)
    {
        cursorCol = Math.max(cursorCol - n, 0);
    }

    // Mode and scroll region management
    /**
     * Enables/disables origin mode.
     * After changing the mode, the cursor moves to the home position.
     */
    public void setOriginMode(boolean on)
    {
        this.originMode = on;
        cursorHome();
    }

    /** Set the scroll region (rows inclusive). */
    public void setScrollRegion(int top, int bottom)
    {
        if (top < 0)
	    top = 0;
        if (bottom >= rows)
	    bottom = rows - 1;
        if (top > bottom)
	    return; // invalid
        this.topMargin = top;
        this.bottomMargin = bottom;
        // Cursor may be outside the region — adjust
        cursorHome();
    }

    // Text output
    /**
     * Outputs a single character at the current cursor position.
     * Operates in overwrite mode; after output, the cursor moves right.
     * If the right boundary is exceeded, an automatic line wrap occurs.
     * The special character '\n' causes a line feed.
     */
    public void writeChar(char ch)
    {
        if (ch == '\n')
	{
            newLine();
            return;
        }
        // If cursor is beyond the right edge, first do a line feed
        if (cursorCol >= cols)
            newLine();
        // Get the buffer line (0-based index)
        final StringBuilder line = buffer.get(cursorRow);
        // Pad the line with spaces if it is shorter than the required position
        while (line.length() < cursorCol) 
            line.append(' ');
        // Write character overwriting existing
        if (line.length() == cursorCol)
            line.append(ch); else
            line.setCharAt(cursorCol, ch);
        // Move cursor right
        cursorCol++;
    }

    /**
     * Fills the text with spaces from the current position.
     * The cursor doesn't change its position.
     * @param num The number of characters to fill
     */
    void fillSpaces(int num)
    {
        // If cursor is beyond the right edge, first do a line feed
        if (cursorCol >= cols)
            newLine();
        // Get the buffer line (0-based index)
        final StringBuilder line = buffer.get(cursorRow);
        // Pad the line with spaces if it is shorter than the required position
        while (line.length() < cursorCol) 
            line.append(' ');
	for(int i = 0;i < num;i++)
        if (line.length() == cursorCol + i)
            line.append(' '); else
            line.setCharAt(cursorCol + i, ' ');
    }


    /** Line feed (LF). */
    public void newLine()
    {
        if (cursorRow < bottomMargin)
	{
            cursorRow++;
            cursorCol = 0;
        } else
	{
            // On the last line of the scroll region — scroll
            scrollUp(1);
            // Cursor stays on bottomMargin
            cursorRow = bottomMargin;
            cursorCol = 0;
        }
    }

    /**
     * Scroll the scroll region up by the specified number of lines.
     * New lines at the bottom of the region are cleared.
     */
    public void scrollUp(int count)
    {
        int topIdx = topMargin - 1;
        int bottomIdx = bottomMargin - 1;
        int regionSize = bottomIdx - topIdx + 1;

        for (int k = 0; k < Math.min(count, regionSize); k++)
	{
            // Remove the topmost line of the region
            buffer.remove(topIdx);
            // Insert a new empty line at the end of the region
            buffer.add(bottomIdx, new StringBuilder());
        }
    }

    private int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(max, value));
    }

        @Override public int getHotPointX()
    {
	return cursorCol;
    }

    @Override public int getHotPointY()
    {
	return cursorRow;
    }
    
    @Override public int getLineCount()
    {
	return Math.max(buffer.size(), 1);
    }

    @Override public String getLine(int index)
    {
	if (index < 0 || index >= buffer.size())
	    return "";
			return new String(buffer.get(index));
    }
}
