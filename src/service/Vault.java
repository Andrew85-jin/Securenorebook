package service;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Objects;

public class Vault {
    private static final int SALT_LEN = 16;
    private static final int NONCE_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final int TAG_BYTES = TAG_BITS /8;
    private static final int ITERATIONS = 600_000;
    private static final int KEY_BITS = 256;

    private static final byte[] MAGIC = {'S','N','B','K',};
    private static final byte   VERSION = 1;

    static final int HEADER_LEN = MAGIC.length + 1 + SALT_LEN + NONCE_LEN;

    private static final SecureRandom RANDOM = new SecureRandom();

    private Vault() {}

    static SecretKey deriveKey(char[] password, byte[] salt){
        Objects.requireNonNull(password, "password повинен бути ");
        if(salt == null || salt.length != SALT_LEN){
            throw new IllegalArgumentException("сіль мусить бути " + SALT_LEN + " байтів");
        }
        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
            byte[] keyBytes;
            try {
                keyBytes = secretKeyFactory.generateSecret(spec).getEncoded();
            }finally {
                spec.clearPassword();
            }
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("KDF недоступний", e);
        }
    }
    private static byte[] gcmEncrypt(SecretKey key, byte[] nonce, byte[] plaintext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            return cipher.doFinal(plaintext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM недоступний", e);
        }
    }

    public static byte[] seal(byte[] plaintext, char[] password) {
        Objects.requireNonNull(plaintext,  "дані не можуть бути null");
        Objects.requireNonNull(password, "парольна фраза не може бути null");


        byte[] salt  = new byte[SALT_LEN];
        byte[] nonce = new byte[NONCE_LEN];
        RANDOM.nextBytes(salt);
        RANDOM.nextBytes(nonce);


        SecretKey key = deriveKey(password, salt);


        byte[] ciphertext = gcmEncrypt(key, nonce, plaintext);


        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
             DataOutputStream out = new DataOutputStream(buffer)) {

            out.write(MAGIC);
            out.writeByte(VERSION);
            out.write(salt);
            out.write(nonce);
            out.write(ciphertext);

            return buffer.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("не вдалося зібрати контейнер", e);
        }
    }

    private static byte[] gcmDecrypt(SecretKey key, byte[] nonce, byte[] ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            return cipher.doFinal(ciphertext);
        } catch (AEADBadTagException e) {
            throw new VaultException("невірний пароль або файл пошкоджено", e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM недоступний", e);
        }
    }

    public static byte[] open(byte[] container, char[] passphrase) {
        Objects.requireNonNull(container,  "контейнер не може бути null");
        Objects.requireNonNull(passphrase, "парольна фраза не може бути null");


        if (container.length < HEADER_LEN + TAG_BYTES) {
            throw new CorruptedVaultException("файл занадто короткий, щоб бути сховищем");
        }

        byte[] salt   = new byte[SALT_LEN];
        byte[] nonce  = new byte[NONCE_LEN];
        byte[] ciphertext;


        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(container))) {

            byte[] magic = new byte[MAGIC.length];
            in.readFully(magic);
            if (!Arrays.equals(magic, MAGIC)) {
                throw new CorruptedVaultException("це не файл сховища");
            }

            byte version = in.readByte();
            if (version != VERSION) {
                throw new CorruptedVaultException("непідтримувана версія: " + version);
            }

            in.readFully(salt);
            in.readFully(nonce);
            ciphertext = in.readAllBytes();

        } catch (EOFException e) {
            throw new CorruptedVaultException("заголовок обірваний", e);
        } catch (IOException e) {
            throw new IllegalStateException("не вдалося прочитати контейнер", e);
        }


        SecretKey key = deriveKey(passphrase, salt);
        return gcmDecrypt(key, nonce, ciphertext);
    }

}
