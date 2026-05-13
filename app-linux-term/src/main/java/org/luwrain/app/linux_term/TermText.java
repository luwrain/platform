
package org.luwrain.app.term;

import java.util.ArrayList;
import java.util.List;

/**
 * Эмуляция ANSI-терминала.
 * Строки хранятся в списке (одна строка — один StringBuilder).
 * Позиция курсора отсчитывается от 1 (строка, столбец).
 * Параметры leftUpperRow/Col управляют виртуальным началом координат в origin mode.
 */
public class TermText
{
    // Буфер строк терминала
    private final List<StringBuilder> buffer;
    // Размеры экрана
    private final int rows;
    private final int cols;

    // Текущая позиция курсора (1-based)
    private int cursorRow;
    private int cursorCol;

    // Положение левого верхнего угла (начало координат при origin mode)
    private int originRow;
    private int originCol;
    private boolean originMode;

    // Область прокрутки (topMargin, bottomMargin включительно, 1-based)
    private int topMargin;
    private int bottomMargin;

    /**
     * Создаёт терминал заданных размеров.
     * @param rows количество строк
     * @param cols количество столбцов
     */
    public TermText(int rows, int cols)
    {
        this.rows = rows;
        this.cols = cols;
        this.buffer = new ArrayList<>(rows);
        for (int i = 0; i < rows; i++) {
            buffer.add(new StringBuilder());
        }
        this.cursorRow = 1;
        this.cursorCol = 1;
        this.originRow = 1;
        this.originCol = 1;
        this.originMode = false;
        this.topMargin = 1;
        this.bottomMargin = rows;
    }

    // -------------------------------------------------------------------------
    // Команды позиционирования курсора
    // -------------------------------------------------------------------------

    /**
     * Аналог CUP (Cursor Position).
     * Устанавливает курсор в заданную позицию.
     * Если включён originMode, координаты отсчитываются от (originRow, originCol).
     * Параметр 0 интерпретируется как 1.
     * @param row номер строки (1..rows)
     * @param col номер столбца (1..cols)
     */
    public void cup(int row, int col)
    {
        if (row < 1) row = 1;
        if (col < 1) col = 1;

        int targetRow, targetCol;
        if (originMode)
	{
            targetRow = originRow + row - 1;
            targetCol = originCol + col - 1;
        } else {
            targetRow = row;
            targetCol = col;
        }

        // Ограничение границами экрана
        cursorRow = clamp(targetRow, 1, rows);
        // Столбец разрешено устанавливать и в cols+1? По стандарту можно,
        // но для простоты ограничим шириной экрана.
        cursorCol = clamp(targetCol, 1, cols);
    }

    /**
     * Перемещение курсора в начало координат (home).
     * При originMode — в (originRow, originCol), иначе в (1, 1).
     */
    public void cursorHome() {
        if (originMode) {
            cup(1, 1); // пересчитает через origin
        } else {
            cursorRow = 1;
            cursorCol = 1;
        }
    }

    /** Перемещение вверх на n строк. */
    public void cursorUp(int n) {
        int maxUp = (originMode ? topMargin : 1);
        cursorRow = Math.max(cursorRow - n, maxUp);
    }

    /** Перемещение вниз на n строк. */
    public void cursorDown(int n) {
        int maxDown = (originMode ? bottomMargin : rows);
        cursorRow = Math.min(cursorRow + n, maxDown);
    }

    /** Перемещение вправо на n столбцов. */
    public void cursorRight(int n) {
        cursorCol = Math.min(cursorCol + n, cols);
    }

    /** Перемещение влево на n столбцов. */
    public void cursorLeft(int n) {
        cursorCol = Math.max(cursorCol - n, 1);
    }

    // -------------------------------------------------------------------------
    // Управление режимами и областью прокрутки
    // -------------------------------------------------------------------------

    /**
     * Включает/выключает origin mode.
     * После изменения режима курсор перемещается в home позицию.
     */
    public void setOriginMode(boolean on)
    {
        this.originMode = on;
        cursorHome();
    }

    /** Установить область прокрутки (строки включительно). */
    public void setScrollRegion(int top, int bottom) {
        if (top < 1) top = 1;
        if (bottom > rows) bottom = rows;
        if (top > bottom) return; // некорректно
        this.topMargin = top;
        this.bottomMargin = bottom;
        // Курсор может стать вне области — скорректируем
        cursorHome();
    }

    // -------------------------------------------------------------------------
    // Вывод текста
    // -------------------------------------------------------------------------

    /**
     * Выводит один символ в текущую позицию курсора.
     * Действует в режиме замены (overwrite), после вывода курсор сдвигается вправо.
     * При выходе за правую границу происходит автоматический перенос строки.
     * Спецсимвол '\n' вызывает перевод строки.
     */
    public void writeChar(char ch)
    {
        if (ch == '\n')
	{
            newLine();
            return;
        }

        // Если курсор находится за правой границей, сначала перевод строки
        if (cursorCol > cols)
	{
            newLine();
        }

        // Получаем строку буфера (индекс 0-based)
        StringBuilder line = buffer.get(cursorRow - 1);

        // Дополняем строку пробелами, если она короче нужной позиции
        while (line.length() < cursorCol - 1) {
            line.append(' ');
        }

        // Запись символа поверх существующего
        if (line.length() == cursorCol - 1)
	{
            line.append(ch);
        } else
	{
            line.setCharAt(cursorCol - 1, ch);
        }

        // Перемещаем курсор вправо
        cursorCol++;
    }

    /** Вывод строки посимвольно. */
    public void writeString(String s)
    {
        for (int i = 0; i < s.length(); i++) {
            writeChar(s.charAt(i));
        }
    }

    /** Перевод строки (LF). */
    public void newLine()
    {
        if (cursorRow < bottomMargin)
	{
            cursorRow++;
            cursorCol = 1;
        } else
	{
            // На последней строке области прокрутки – скроллим
            scrollUp(1);
            // Курсор остаётся на bottomMargin
            cursorRow = bottomMargin;
            cursorCol = 1;
        }
    }

    /**
     * Скроллинг области прокрутки вверх на указанное число строк.
     * Новые строки внизу области очищаются.
     */
    public void scrollUp(int count)
    {
        int topIdx = topMargin - 1;
        int bottomIdx = bottomMargin - 1;
        int regionSize = bottomIdx - topIdx + 1;

        for (int k = 0; k < Math.min(count, regionSize); k++)
	{
            // Удаляем самую верхнюю строку области
            buffer.remove(topIdx);
            // Вставляем новую пустую строку в конец области
            buffer.add(bottomIdx, new StringBuilder());
        }
    }

    // -------------------------------------------------------------------------
    // Вспомогательные методы
    // -------------------------------------------------------------------------

    private int clamp(int value, int min, int max)
    {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Возвращает видимое содержимое экрана (строки обрезаны/дополнены до cols).
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

    // Getters (при необходимости)
    public int getCursorRow() { return cursorRow; }
    public int getCursorCol() { return cursorCol; }
    public int getOriginRow() { return originRow; }
    public int getOriginCol() { return originCol; }
    public boolean isOriginMode() { return originMode; }
}
