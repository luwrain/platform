
package org.luwrain.app.linux_term;

import java.util.ArrayList;
import java.util.List;

import org.luwrain.core.*;

/**
 * Text content of ANSI terminal emulation.
 * Lines are stored in a list (one line — one StringBuilder).
 * Cursor position is 1-based (row, column).
 * Parameters originRow/originCol control the virtual origin in origin mode.
 */
public class TermText implements Lines, HotPoint
{
    // Terminal line buffer
    private final List<StringBuilder> buffer;
    // Screen dimensions
    private final int rows, cols;

    // Current cursor position (1-based)
    private int cursorRow, cursorCol;

    // Position of the top-left corner (origin in origin mode)
    private int originRow, originCol;
    private boolean originMode;

    // Scroll region (topMargin, bottomMargin inclusive, 1-based)
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
     * A parameter of 0 is treated as 1.
     * @param row row number (1..rows)
     * @param col column number (1..cols)
     */
    void setCursorPos(int row, int col)
    {
        if (row < 1)
	    row = 1;
        if (col < 1)
	    col = 1;
        final int targetRow, targetCol;
        if (originMode)
	{
            targetRow = originRow + row - 1;
            targetCol = originCol + col - 1;
        } else
	{
            targetRow = row;
            targetCol = col;
        }

        // Clamp to screen boundaries
        cursorRow = clamp(targetRow, 1, rows);
        // Column is allowed to be set to cols+1? According to the standard it is,
        // but for simplicity we limit to screen width.
        cursorCol = clamp(targetCol, 1, cols);
    }

    /**
     * Moves cursor to home position.
     * In originMode — to (originRow, originCol), otherwise to (1, 1).
     */
    public void cursorHome() {
        if (originMode) {
            setCursorPos(1, 1); // recalculates via origin
        } else {
            cursorRow = 1;
            cursorCol = 1;
        }
    }

    /** Move up by n lines. */
    public void cursorUp(int n) {
        int maxUp = (originMode ? topMargin : 1);
        cursorRow = Math.max(cursorRow - n, maxUp);
    }

    /** Move down by n lines. */
    public void cursorDown(int n) {
        int maxDown = (originMode ? bottomMargin : rows);
        cursorRow = Math.min(cursorRow + n, maxDown);
    }

    /** Move right by n columns. */
    public void cursorRight(int n) {
        cursorCol = Math.min(cursorCol + n, cols);
    }

    /** Move left by n columns. */
    public void cursorLeft(int n) {
        cursorCol = Math.max(cursorCol - n, 1);
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
    public void setScrollRegion(int top, int bottom) {
        if (top < 1) top = 1;
        if (bottom > rows) bottom = rows;
        if (top > bottom) return; // invalid
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
        if (cursorCol > cols)
            newLine();
        // Get the buffer line (0-based index)
        final StringBuilder line = buffer.get(cursorRow - 1);
        // Pad the line with spaces if it is shorter than the required position
        while (line.length() < cursorCol - 1) 
            line.append(' ');
	// Handling backspace
	if (ch == '\b')
	{
	                line.setCharAt(cursorCol - 2, ' ');
			cursorCol--;
			return;
	}
        // Write character overwriting existing
        if (line.length() == cursorCol - 1)
            line.append(ch); else
            line.setCharAt(cursorCol - 1, ch);
        // Move cursor right
        cursorCol++;
    }

    /** Output a string character by character. */
    void writeString(String s)
    {
        for (int i = 0; i < s.length(); i++) 
            writeChar(s.charAt(i));
    }

    /** Line feed (LF). */
    public void newLine()
    {
        if (cursorRow < bottomMargin)
	{
            cursorRow++;
            cursorCol = 1;
        } else
	{
            // On the last line of the scroll region — scroll
            scrollUp(1);
            // Cursor stays on bottomMargin
            cursorRow = bottomMargin;
            cursorCol = 1;
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

    /**
     * Returns the visible screen content (lines trimmed/padded to cols).
     */
    public String getDisplay()
    {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            StringBuilder line = buffer.get(r);
            int len = line.length();
            sb.append(line.substring(0, Math.min(len, cols)));
            for (int c = len; c < cols; c++) {
                sb.append(' ');
            }
            if (r < rows - 1) sb.append('\n');
        }
        return sb.toString();
    }

        @Override public int getHotPointX()
    {
	return cursorCol - 1;
    }

    @Override public int getHotPointY()
    {
	return cursorRow - 1;
    }
    

    public int getOriginRow() { return originRow; }
    public int getOriginCol() { return originCol; }
    public boolean isOriginMode() { return originMode; }

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
