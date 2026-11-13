package service;

import model.Note;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class NoteBookFile {
    private NoteBookFile(){

    }
    public static boolean exists(Path path){
        return Files.exists(path);
    }

    public static void save(Path path, List<Note> notes, char[] password){
        byte[] plain = NoteSerializer.serialize(notes);
        byte[] container = Vault.seal(plain,password);

        try {
            Files.write(path, container);
        }catch (IOException e){
            throw new UncheckedIOException("невдалося записати " + path , e);
        }
    }
    public static List<Note> load(Path path, char[] passphrase) {
        try {
            byte[] container = Files.readAllBytes(path);
            byte[] plain = Vault.open(container, passphrase);
            return NoteSerializer.deserialize(plain);
        } catch (IOException e) {
            throw new UncheckedIOException("не вдалося прочитати " + path, e);
        }
    }
}
