import model.Note;
import service.NoteBookFile;
import service.VaultException;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Path PATH = Path.of("notes.vault");
    private static final int MIN_PASSWORD_LEN = 8;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        printStartMenu();
        String input = sc.nextLine().trim();

        List<Note> notes;
        char[] password;

        if (input.equals("1")) {
            if (NoteBookFile.exists(PATH)) {
                System.out.println("Сховище вже існує — відкрийте його (пункт 2) або видаліть файл вручну");
                return;
            }
            password = createPassword(sc);
            if (password == null) {
                return;
            }
            notes = new ArrayList<>();
            try {
                NoteBookFile.save(PATH, notes, password);
            } catch (UncheckedIOException e) {
                System.out.println("Не вдалося створити сховище: " + e.getMessage());
                Arrays.fill(password, '\0');
                return;
            }
            System.out.println("Сховище створено: " + PATH);

        } else if (input.equals("2")) {
            password = askPassword(sc, "Пароль: ");
            try {
                notes = NoteBookFile.load(PATH, password);
            } catch (VaultException e) {
                System.out.println("Невірний пароль або файл пошкоджено");
                Arrays.fill(password, '\0');
                return;
            } catch (UncheckedIOException e) {
                System.out.println("Файл не знайдено або недоступний");
                Arrays.fill(password, '\0');
                return;
            }
            System.out.println("Відкрито нотаток: " + notes.size());

        } else {
            return;
        }

        boolean running = true;
        while (running) {
            printMenu();
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> showAll(notes);
                case "2" -> addNote(sc, notes, password);
                case "3" -> editNote(sc, notes, password);
                case "4" -> deleteNote(sc, notes, password);
                case "0" -> running = false;
                default -> System.out.println("Невідомий пункт: " + choice);
            }
        }

        Arrays.fill(password, '\0');
        System.out.println("До побачення");
    }

    private static void printStartMenu() {
        System.out.println("1. Створити нове сховище");
        System.out.println("2. Відкрити існуюче");
        System.out.println("0. Вихід");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("1. Показати всі нотатки");
        System.out.println("2. Додати нотатку");
        System.out.println("3. Редагувати нотатку");
        System.out.println("4. Видалити нотатку");
        System.out.println("0. Вийти");
    }

    private static void showAll(List<Note> notes) {
        if (notes.isEmpty()) {
            System.out.println("Сховище порожнє");
            return;
        }
        for (int i = 0; i < notes.size(); i++) {
            Note n = notes.get(i);
            System.out.println((i + 1) + ". " + n.getTitle() + " — " + shorten(n.getContent()));
        }
    }

    private static void addNote(Scanner sc, List<Note> notes, char[] password) {
        System.out.print("Заголовок: ");
        String title = sc.nextLine().trim();

        System.out.print("Текст: ");
        String content = sc.nextLine();

        if (title.isEmpty()) {
            System.out.println("Заголовок не може бути порожнім");
            return;
        }

        notes.add(Note.newNote(title, content));
        persist(notes, password);
        System.out.println("Додано");
    }

    private static void editNote(Scanner sc, List<Note> notes, char[] password) {
        if (notes.isEmpty()) {
            System.out.println("Сховище порожнє — немає що редагувати");
            return;
        }
        showAll(notes);

        int idx = askIndex(sc, notes.size());
        if (idx < 0) {
            return;
        }

        Note n = notes.get(idx);

        System.out.print("Новий заголовок [" + n.getTitle() + "]: ");
        String title = sc.nextLine().trim();
        if (title.isEmpty()) {
            title = n.getTitle();
        }

        System.out.print("Новий текст [" + shorten(n.getContent()) + "]: ");
        String content = sc.nextLine();
        if (content.isEmpty()) {
            content = n.getContent();
        }

        n.edit(title, content);
        persist(notes, password);
        System.out.println("Змінено");
    }

    private static void deleteNote(Scanner sc, List<Note> notes, char[] password) {
        if (notes.isEmpty()) {
            System.out.println("Сховище порожнє — немає що видаляти");
            return;
        }
        showAll(notes);

        int idx = askIndex(sc, notes.size());
        if (idx < 0) {
            return;
        }

        notes.remove(idx);
        persist(notes, password);
        System.out.println("Видалено");
    }

    private static int askIndex(Scanner sc, int size) {
        System.out.print("Номер (1-" + size + "): ");
        String raw = sc.nextLine().trim();
        try {
            int idx = Integer.parseInt(raw) - 1;
            if (idx < 0 || idx >= size) {
                System.out.println("Немає такого номера");
                return -1;
            }
            return idx;
        } catch (NumberFormatException e) {
            System.out.println("Це не число: " + raw);
            return -1;
        }
    }

    private static void persist(List<Note> notes, char[] password) {
        try {
            NoteBookFile.save(PATH, notes, password);
        } catch (UncheckedIOException e) {
            System.out.println("Не вдалося зберегти: " + e.getMessage());
        }
    }

    private static String shorten(String s) {
        return s.length() <= 40 ? s : s.substring(0, 40) + "…";
    }

    private static char[] askPassword(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().toCharArray();
    }

    private static char[] createPassword(Scanner scanner) {
        char[] first = askPassword(scanner, "Придумайте пароль: ");
        char[] second = askPassword(scanner, "Повторіть пароль: ");

        if (first.length < MIN_PASSWORD_LEN) {
            System.out.println("Пароль занадто короткий: мінімум " + MIN_PASSWORD_LEN + " символів");
            Arrays.fill(first, '\0');
            Arrays.fill(second, '\0');
            return null;
        }

        if (!Arrays.equals(first, second)) {
            System.out.println("Паролі не збігаються");
            Arrays.fill(first, '\0');
            Arrays.fill(second, '\0');
            return null;
        }

        Arrays.fill(second, '\0');
        return first;
    }
}
