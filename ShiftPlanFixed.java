import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ShiftPlanFixed {

    private static final double MAX_HOURS = 10.0;
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static int failedChecks = 0;

    public static void main(String[] args) {
        List<Brigade> brigades = new ArrayList<>();
        brigades.add(new Brigade("Иванов", 11.0, true));
        brigades.add(new Brigade("Петров", 0.0, true));
        brigades.add(new Brigade("Сидоров", 4.5, false));
        brigades.add(new Brigade("Козлов", 8.0, true));

        try {
            runTests();

            String result = make(brigades, 2);

            // Путь к файлу задаётся в main, а не внутри make.
            Files.writeString(
                    Path.of("наряд.txt"),
                    result,
                    StandardCharsets.UTF_8
            );

            System.out.println("Сформированный наряд:");
            System.out.println(result);
            System.out.println("Журнал: "
                    + DATE_FORMAT.format(LocalDate.now())
                    + " наряд сформирован");

        } catch (IOException e) {
            System.err.println(
                    "Не удалось записать наряд в файл: "
                            + e.getMessage()
            );
        } catch (IllegalArgumentException e) {
            System.err.println("Не удалось сформировать наряд: "
                    + e.getMessage());
        } catch (AssertionError e) {
            System.err.println("Обнаружены ошибки в тестах: "
                    + e.getMessage());
        }
    }

    /**
     * Формирует наряд и возвращает его в виде строки.
     * Метод не выполняет файловых операций.
     */
    public static String make(List<Brigade> brigades, int need) {

        if (brigades == null) {
            throw new IllegalArgumentException(
                    "Список бригад не должен быть null"
            );
        }

        if (need <= 0) {
            throw new IllegalArgumentException(
                    "Количество бригад должно быть положительным"
            );
        }

        // Исходный список не изменяется.
        List<Brigade> suitable = new ArrayList<>();
        List<String> rejected = new ArrayList<>();

        for (Brigade brigade : brigades) {

            if (brigade == null) {
                rejected.add("Неизвестная бригада: запись равна null");
                continue;
            }

            if (!Double.isFinite(brigade.hoursPerShift)
                    || brigade.hoursPerShift < 0) {
                rejected.add(brigade.lastName
                        + " — некорректные данные о количестве часов");
                continue;
            }

            if (!brigade.admitted) {
                rejected.add(brigade.lastName + " — не допущен");
                continue;
            }

            if (brigade.hoursPerShift > MAX_HOURS) {
                rejected.add(brigade.lastName
                        + " — превышены допустимые часы ("
                        + brigade.hoursPerShift + ")");
                continue;
            }

            suitable.add(brigade);
        }

        if (suitable.size() < need) {
            String details = rejected.isEmpty()
                    ? " Причины фильтрации отсутствуют."
                    : " Отфильтрованные бригады: "
                            + String.join("; ", rejected);

            throw new IllegalArgumentException(
                    "Недостаточно подходящих бригад. Доступно: "
                            + suitable.size()
                            + ", требуется: " + need + "."
                            + details
            );
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < need; i++) {
            result.append(suitable.get(i).lastName)
                    .append("; ");
        }

        return result.toString();
    }

    /**
     * Формирует наряд и записывает его в переданный Writer.
     * Вызывающий код сам отвечает за создание и закрытие Writer.
     */
    public static String make(
            List<Brigade> brigades, int need, Writer writer)
            throws IOException {

        if (writer == null) {
            throw new IllegalArgumentException(
                    "Writer не должен быть null"
            );
        }

        String result = make(brigades, need);
        writer.write(result);
        return result;
    }

    // Проверка без немедленного прерывания тестирования.
    public static void check(String name, boolean condition) {
        if (condition) {
            System.out.println("[OK] " + name);
        } else {
            System.out.println("[FAIL] " + name);
            failedChecks++;
        }
    }

    // Проверка ожидаемого исключения.
    public static void checkException(
            String name, Runnable action) {

        boolean exceptionThrown = false;

        try {
            action.run();
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        } catch (Exception e) {
            // Неожиданное исключение также означает провал теста.
        }

        check(name, exceptionThrown);
    }

    public static void runTests() {
        failedChecks = 0;

        List<Brigade> testBrigades = new ArrayList<>();
        testBrigades.add(new Brigade("Иванов", 11.0, true));
        testBrigades.add(new Brigade("Петров", 0.0, true));
        testBrigades.add(new Brigade("Сидоров", 4.5, false));
        testBrigades.add(new Brigade("Козлов", 8.0, true));

        // Тест 1. Правильный состав наряда.
        String result = make(testBrigades, 2);
        check("Правильный состав наряда",
                result.equals("Петров; Козлов; "));

        // Тест 2. Исходный список не изменяется.
        check("Исходный список не изменён",
                testBrigades.size() == 4);

        // Тест 3. Недостаточное количество бригад.
        checkException("Проверка недостаточного количества",
                () -> make(testBrigades, 3));

        // Тест 4. Отрицательное количество бригад.
        checkException("Проверка отрицательного количества",
                () -> make(testBrigades, -1));

        // Тест 5. Передача null вместо списка.
        checkException("Проверка null",
                () -> make(null, 1));

        // Тест 6. Отрицательное количество часов.
        List<Brigade> invalidHours = new ArrayList<>();
        invalidHours.add(new Brigade("Тест", -5.0, true));

        checkException("Проверка отрицательных часов",
                () -> make(invalidHours, 1));

        // Тест 7. Проверка NaN.
        List<Brigade> nanHours = new ArrayList<>();
        nanHours.add(new Brigade(
                "Тест NaN", Double.NaN, true));

        checkException("Проверка NaN",
                () -> make(nanHours, 1));

        // Тест 8. Проверка положительной бесконечности.
        List<Brigade> infiniteHours = new ArrayList<>();
        infiniteHours.add(new Brigade(
                "Тест Infinity", Double.POSITIVE_INFINITY, true));

        checkException("Проверка положительной бесконечности",
                () -> make(infiniteHours, 1));

        // Тест 9. Проверка отрицательной бесконечности.
        List<Brigade> negativeInfinityHours = new ArrayList<>();
        negativeInfinityHours.add(new Brigade(
                "Тест -Infinity", Double.NEGATIVE_INFINITY, true));

        checkException("Проверка отрицательной бесконечности",
                () -> make(negativeInfinityHours, 1));

        // Тест 10. Запись через переданный Writer, без реального файла.
        StringWriter testWriter = new StringWriter();
        try {
            String writerResult = make(
                    testBrigades, 2, testWriter);

            check("Проверка записи через Writer",
                    writerResult.equals("Петров; Козлов; ")
                            && testWriter.toString().equals(
                                    "Петров; Козлов; "));
        } catch (IOException e) {
            check("Проверка записи через Writer", false);
        }

        // Итог: сообщаем обо всех проваленных проверках сразу.
        if (failedChecks > 0) {
            throw new AssertionError(
                    "Количество проваленных проверок: "
                            + failedChecks
            );
        }

        System.out.println("Все тесты выполнены успешно.");
    }
}

class Brigade {

    String lastName;
    double hoursPerShift;
    boolean admitted;

    Brigade(String lastName, double hoursPerShift,
            boolean admitted) {

        this.lastName = lastName;
        this.hoursPerShift = hoursPerShift;
        this.admitted = admitted;
    }
}
