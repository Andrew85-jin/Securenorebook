package service;

import model.Note;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

public class VaultDemo {
    public static void main(String[] args){
        SecureRandom rnd = new SecureRandom();
        char[] pass = "correct horse battery staple".toCharArray();
        HexFormat hex = HexFormat.of();

        byte[] saltA = new byte[16];
        rnd.nextBytes(saltA);

        long t0 = System.nanoTime();
        SecretKey k1 = Vault.deriveKey(pass, saltA);
        long t1 = System.nanoTime();

        SecretKey k2 = Vault.deriveKey(pass, saltA);


        byte[] saltB = new byte[16];
        rnd.nextBytes(saltB);
        SecretKey k3 = Vault.deriveKey(pass, saltB);


        System.out.println("key1  = " + hex.formatHex(k1.getEncoded()));
        System.out.println("key2  = " + hex.formatHex(k2.getEncoded()));
        System.out.println("key3  = " + hex.formatHex(k3.getEncoded()));
        System.out.println("key1 == key2 ? " + Arrays.equals(k1.getEncoded(), k2.getEncoded()));
        System.out.println("key1 == key3 ? " + Arrays.equals(k1.getEncoded(), k3.getEncoded()));
        System.out.println("time  = " + (t1 - t0) / 1_000_000 + " ms");

        System.out.println("========================================");

        byte[] plain = "text".getBytes(StandardCharsets.UTF_8);
        char[] pass2  = "correct horse".toCharArray();

        byte[] c1 = Vault.seal(plain, pass2);
        byte[] c2 = Vault.seal(plain, pass2);

        HexFormat hex2 = HexFormat.of();

        System.out.println("len1   = " + c1.length);
        System.out.println("len2   = " + c2.length);
        System.out.println("equal  = " + Arrays.equals(c1, c2));
        System.out.println("header = " + hex2.formatHex(Arrays.copyOf(c1, Vault.HEADER_LEN)));
        System.out.println("tag    = " + hex2.formatHex(Arrays.copyOfRange(c1, c1.length - 16, c1.length)));

        System.out.println("============================");

        byte[] plain3 = "Тестова нотатка для перевірки цілісності".getBytes(StandardCharsets.UTF_8);
        char[] pass3  = "correct horse".toCharArray();

        byte[] container = Vault.seal(plain3, pass3);
        System.out.println("len = " + container.length);

        byte[] back = Vault.open(container, pass3);
        System.out.println("1: " + Arrays.equals(plain3, back));

        try {
            Vault.open(container, "wrong".toCharArray());
            System.out.println("2: ПОМИЛКА — винятку не було");
        } catch (VaultException e) {
            System.out.println("2: " + e.getMessage());
        }

        byte[] broken = container.clone();
        broken[broken.length - 1] ^= 0x01;
        try {
            Vault.open(broken, pass3);
            System.out.println("3: ПОМИЛКА — винятку не було");
        } catch (VaultException e) {
            System.out.println("3: " + e.getMessage());
        }

        System.out.println("================================");

        Path p = Path.of("demo.vault");

        // --- 1. порожній записник ---
        NoteBookFile.save(p, List.of(), "pass".toCharArray());
        System.out.println("empty = " + NoteBookFile.load(p, "pass".toCharArray()).size());

        Note original = Note.newNote("Перша нотатка", "Текст для перевірки");
        NoteBookFile.save(p, List.of(original), "pass".toCharArray());

        List<Note> restored = NoteBookFile.load(p, "pass".toCharArray());
        System.out.println("size    = " + restored.size());
        System.out.println("title   = " + restored.get(0).getTitle());
        System.out.println("content = " + restored.get(0).getContent());
        System.out.println("id ok   = " + original.getId().equals(restored.get(0).getId()));

        // --- 3. хибний пароль ---
        try {
            NoteBookFile.load(p, "wrong".toCharArray());
            System.out.println("ПОМИЛКА — винятку не було");
        } catch (VaultException e) {
            System.out.println("wrong   = " + e.getMessage());
        }

        // --- 4. прибирання ---
        try {
            Files.deleteIfExists(p);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
