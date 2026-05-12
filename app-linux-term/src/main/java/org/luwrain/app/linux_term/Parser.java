
package org.luwrain.app.linux_term;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Parser
{

    // Класс, представляющий распарсенную команду
    public static class AnsiCommand {
        public final String rawSequence;
        public final char finalChar;
        public final String privateMarker;
        public final List<Integer> parameters;
        public final String description;

        public AnsiCommand(String rawSequence, char finalChar, String privateMarker, List<Integer> parameters, String description) {
            this.rawSequence = rawSequence;
            this.finalChar = finalChar;
            this.privateMarker = privateMarker;
            this.parameters = parameters;
            this.description = description;
        }

        @Override
        public String toString() {
            return String.format("Команда: %-30s | Маркер: '%s' | Аргументы: %-10s | Финал: '%c' | Сырая: %s",
                    description, privateMarker, parameters, finalChar, 
                    rawSequence.replace("\033", "\\e")); // заменяем непечатный ESC для вывода
        }
    }

    // Состояния нашего автомата
    private enum State {
        NORMAL,       // Обычный текст
        ESCAPE,       // Получили \033
        CSI_PARAM     // Внутри \033[ (Control Sequence Introducer)
    }

    /**
     * Основной метод парсинга входного потока (строки)
     */
    public List<AnsiCommand> parse(String input) {
        List<AnsiCommand> commands = new ArrayList<>();
        State state = State.NORMAL;

        StringBuilder rawBuffer = new StringBuilder();
        StringBuilder paramBuffer = new StringBuilder();
        String privateMarker = "";

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            switch (state) {
                case NORMAL:
                    if (c == '\033') { // ESCAPE (0x1B)
                        state = State.ESCAPE;
                        rawBuffer.setLength(0);
                        rawBuffer.append(c);
                    }
                    // Обычные символы просто игнорируем (или можно выводить на экран)
                    break;

                case ESCAPE:
                    rawBuffer.append(c);
                    if (c == '[') {
                        state = State.CSI_PARAM;
                        paramBuffer.setLength(0);
                        privateMarker = "";
                    } else if (c == ']') {
                        // OSC (Operating System Command) - например, смена заголовка окна.
                        // Для упрощения примера сбрасываем, но в реальном TUI здесь нужен свой State.
                        state = State.NORMAL; 
                    } else {
                        // Одиночные ESC-команды (например ESC M - Reverse Index)
                        state = State.NORMAL;
                    }
                    break;

                case CSI_PARAM:
                    rawBuffer.append(c);
                    
                    // Приватные маркеры (например, ? для режимов DEC)
                    if (c >= 0x3C && c <= 0x3F) { 
                        privateMarker = String.valueOf(c);
                    } 
                    // Числа и разделитель (точка с запятой)
                    else if ((c >= '0' && c <= '9') || c == ';') {
                        paramBuffer.append(c);
                    } 
                    // Финальный символ (определяет тип команды, буквы от @ до ~)
                    else if (c >= 0x40 && c <= 0x7E) {
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
        return commands;
    }

    /**
     * Парсит строку вида "31;1;;4" в список чисел.
     * Пропущенные значения неявно считаются нулями (стандартное поведение xterm).
     */
    private List<Integer> parseParameters(String paramStr) {
        if (paramStr.isEmpty()) {
            return new ArrayList<>(); // Без аргументов
        }
        return Arrays.stream(paramStr.split(";", -1))
                .map(s -> s.isEmpty() ? 0 : Integer.parseInt(s))
                .collect(Collectors.toList());
    }

    /**
     * "Словарь" команд. Переводит машинные коды в человекочитаемый смысл.
     */
    private String resolveCommandDescription(char finalChar, String marker, List<Integer> params) {
        if ("?".equals(marker)) {
            switch (finalChar) {
                case 'h': return "DEC SET (Включить режим)";
                case 'l': return "DEC RESET (Выключить режим)";
                default: return "DEC Private Command";
            }
        }

        switch (finalChar) {
            case 'A': return "Cursor Up (Курсор вверх)";
            case 'B': return "Cursor Down (Курсор вниз)";
            case 'C': return "Cursor Forward (Курсор вправо)";
            case 'D': return "Cursor Back (Курсор влево)";
            case 'H': 
            case 'f': return "Cursor Position (Перемещение курсора)";
            case 'J': return "Erase in Display (Очистка экрана)";
            case 'K': return "Erase in Line (Очистка строки)";
            case 'm': 
                // m - самая популярная команда (цвета, жирность)
                return "SGR (Стиль/Цвет текста)";
            case 's': return "Save Cursor Position (Сохранить курсор)";
            case 'u': return "Restore Cursor Pos (Восстановить курсор)";
            default: return "Unknown/Other Control Sequence";
        }
    }

    // ==========================================
    // ТЕСТОВЫЙ ЗАПУСК
    // ==========================================
    public static void main(String[] args) {
        // Имитируем поток данных с терминала (смесь текста и команд)
        // \033 = ESC
        String terminalStream = 
            "Обычный текст " +
            "\033[31;1m" +       // Красный, жирный
            "Красный текст" + 
            "\033[0m" +          // Сброс
            "\033[?25l" +        // Спрятать курсор
            "\033[12;40H" +      // Курсор на 12 строку, 40 колонку
            "\033[2J" +          // Полная очистка экрана
            "\033[;;3m";         // Тест пропущенных аргументов (эквивалент 0;0;3)

        Parser parser = new Parser();
        List<AnsiCommand> parsedCommands = parser.parse(terminalStream);

        System.out.println("Анализ терминального потока:");
        System.out.println("==========================================================================================");
        for (AnsiCommand cmd : parsedCommands) {
            System.out.println(cmd.toString());
        }
    }
}
