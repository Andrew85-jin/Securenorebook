package service;

import java.security.SecureRandom;
import java.util.Locale;

public class Benchmark {
    private static final int[] SIZES = {0, 1_024, 100 * 1_024, 1_048_576, 10 * 1_048_576};
    private static final int REPEATS = 5;
    private static final int WARMUP = 3;

    public static void main(String[] args) {
        char[] password = "correct horse battery staple".toCharArray();

        warmUp(password);

        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        double kdfMs = measureAvg(() -> Vault.deriveKey(password, salt), REPEATS);

        System.out.printf(Locale.ROOT, "%12s %15s %10s %10s %10s%n",
                "розмір, Б", "контейнер, Б", "seal, мс", "open, мс", "AES, мс");
        System.out.println("-".repeat(61));

        double lastSealMs = 0;

        for (int i = 0; i < SIZES.length; i++) {
            int size = SIZES[i];
            byte[] plain = new byte[size];
            byte[] container = Vault.seal(plain, password);

            double sealMs = measureAvg(() -> Vault.seal(plain, password), REPEATS);
            double openMs = measureAvg(() -> Vault.open(container, password), REPEATS);

            double aesMs = Math.max(0, sealMs - kdfMs);

            System.out.printf(Locale.ROOT, "%12d %15d %10.1f %10.1f %10.1f%n",
                    size, container.length, sealMs, openMs, aesMs);

            if (i == SIZES.length - 1) {
                lastSealMs = sealMs;
            }
        }

        System.out.printf(Locale.ROOT, "%nKDF (PBKDF2, 600 000 ітерацій): %.1f мс%n", kdfMs);
        System.out.printf(Locale.ROOT, "оцінка AES-GCM на 10 МБ:        %.1f мс%n",
                Math.max(0, lastSealMs - kdfMs));
    }

    private static void warmUp(char[] passphrase) {
        byte[] warm = new byte[1_024];
        for (int i = 0; i < WARMUP; i++) {
            Vault.open(Vault.seal(warm, passphrase), passphrase);
        }
    }

    private static double measureAvg(Runnable operation, int repeats) {
        double sum = 0;
        for (int i = 0; i < repeats; i++) {
            long start = System.nanoTime();
            operation.run();
            long end = System.nanoTime();
            sum += (end - start) / 1_000_000.0;
        }
        return sum / repeats;
    }
}
