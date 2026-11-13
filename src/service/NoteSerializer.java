package service;

import model.Note;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;


public class NoteSerializer {
    private static final int MIN_RECORD_BYTES = 40;

    private NoteSerializer(){}

    public static byte[] serialize(java.util.List<Note> notes){
        Objects.requireNonNull(notes, "Список не може бути пустим");

        try(ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(buffer);
        ) {
            out.writeInt(notes.size());

            for (Note n : notes){
                out.writeLong(n.getId().getMostSignificantBits());
                out.writeLong(n.getId().getLeastSignificantBits());

                writeString(out, n.getTitle());
                writeString(out, n.getContent());

                out.writeLong(n.getCreatedAt().toEpochSecond(ZoneOffset.UTC));
                out.writeInt(n.getCreatedAt().getNano());
                out.writeLong(n.getUpdatedAt().toEpochSecond(ZoneOffset.UTC));
                out.writeInt(n.getUpdatedAt().getNano());
            }
            return buffer.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("Неочікувана помилка сереалізації");
        }
    }

    public static List<Note> deserialize(byte[] data){
        try(DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))){
            int count = in.readInt();
            if(count < 0 ){
                throw new CorruptedStoreException("Від'ємна кількість записів" + count);

            }

            if(count > in.available() / MIN_RECORD_BYTES){
                throw new CorruptedStoreException("кількість записів (" + count + ") несумісна з розмірами даних");
            }

            List<Note> result = new ArrayList<>(count);
            for(int i = 0; i < count; i++){
                long msb = in.readLong();
                long lsb = in.readLong();
                UUID id = new UUID(msb, lsb);

                String title = readString(in, "title");
                String content = readString(in, "content");

                long creteSec = in.readLong();
                int createNano = in.readInt();
                long updateSec = in.readLong();
                int updateNano = in.readInt();
                LocalDateTime createdAt = LocalDateTime.ofEpochSecond(creteSec, createNano, ZoneOffset.UTC);
                LocalDateTime updatedAt = LocalDateTime.ofEpochSecond(updateSec, updateNano, ZoneOffset.UTC);

                result.add(new Note(id, title, content, createdAt, updatedAt));
            }
            if (in.available() != 0) {
                throw new CorruptedStoreException("зайві байти в кінці даних");
            }
            return result;
        } catch (EOFException e) {
            throw new CorruptedStoreException("дані обірвані раніше, ніж мали скінчитися" ,e);
        }catch (IOException e){
            throw new IllegalStateException("Неочікувана помилка десеріалізації", e);
        }
    }


    private static void writeString(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }
    private static String readString(DataInputStream in, String field) throws IOException {
        int length = in.readInt();
        if (length < 0) {
            throw new CorruptedStoreException("від'ємна довжина поля " + field + ": " + length);
        }
        if (length > in.available()) {
            throw new CorruptedStoreException(
                    "довжина поля " + field + " більша за залишок даних: " + length);
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
