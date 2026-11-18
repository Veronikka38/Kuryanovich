import java.io.IOException;
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

    public static void main(String[] args) throws IOException {

        List<Brigade> brigades = new ArrayList<>();
        brigades.add(new Brigade("Иванов", 11.0, true));
        brigades.add(new Brigade("Петров", 0.0, true));
        brigades.add(new Brigade("Сидоров", 4.5, false));
        brigades.add(new Brigade("Козлов", 8.0, true));

        runTests();

        System.out.println("Сформированный наряд:");
        System.out.println(make(brigades, 2));

        System.out.println("Журнал: " +
                DATE_FORMAT.format(LocalDate.now()) +
                " наряд сформирован");
    }

    public static String make(List<Brigade> brigades, int need)
            throws IOException {

        if (brigades == null) {
            throw new IllegalArgumentException(
                    "Список бригад не должен быть null");
        }

        if (need <= 0) {
            throw new IllegalArgumentException(
                    "Количество бригад должно быть положительным");
        }

        // Создаём новый список, не изменяя исходный.
        List<Brigade> suitable = new ArrayList<>();

        for (Brigade brigade : brigades) {

            if (brigade == null) {
                throw new IllegalArgumentException(
                        "Список содержит пустую бригаду");
            }

            if (brigade.hoursPerShift < 0 ||
                    Double.isNaN(brigade.hoursPerShift)) {
                throw new IllegalArgumentException(
                        "Количество часов указано некорректно");
            }

            if (brigade.admitted &&
                    brigade.hoursPerShift <= MAX_HOURS) {
                suitable.add(brigade);
            }
        }

        if (suitable.size() < need) {
            throw new IllegalArgumentException(
                    "Недостаточно подходящих бригад. Доступно: "
                    + suitable.size() + ", требуется: " + need);
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < need; i++) {
            result.append(suitable.get(i).lastName)
                    .append("; ");
        }

        // Автоматическое закрытие файла.
        try (var writer = Files.newBufferedWriter(
                Path.of("наряд.txt"),
                StandardCharsets.UTF_8)) {

            writer.write(result.toString());
        }

        return result.toString();
    }

    // Метод автоматической проверки.
    public static void check(String name, boolean condition) {
        if (condition) {
            System.out.println("[OK] " + name);
        } else {
            System.out.println("[FAIL] " + name);
            throw new AssertionError(name);
        }
    }

    // Проверка ожидаемого исключения.
    public static void checkException(
            String name, Runnable action) {

        try {
            action.run();
            check(name, false);
        } catch (IllegalArgumentException e) {
            check(name, true);
        }
    }

    public static void runTests() throws IOException {

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

        // Тест 3. Проверка недостаточного количества бригад.
        checkException("Проверка недостаточного количества",
                () -> {
                    try {
                        make(testBrigades, 3);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        // Тест 4. Проверка отрицательного количества бригад.
        checkException("Проверка отрицательного количества",
                () -> {
                    try {
                        make(testBrigades, -1);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        // Тест 5. Проверка пустого списка (null).
        checkException("Проверка null",
                () -> {
                    try {
                        make(null, 1);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        // Тест 6. Проверка некорректных часов.
        List<Brigade> invalid = new ArrayList<>();
        invalid.add(new Brigade("Тест", -5.0, true));

        checkException("Проверка отрицательных часов",
                () -> {
                    try {
                        make(invalid, 1);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        // Тест 7. Проверка сохранения файла.
        String saved = Files.readString(
                Path.of("наряд.txt"),
                StandardCharsets.UTF_8);

        check("Проверка записи файла",
                saved.equals("Петров; Козлов; "));

        System.out.println("Все тесты выполнены.");
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
