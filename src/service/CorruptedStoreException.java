package service;

public class CorruptedStoreException extends RuntimeException {
    public CorruptedStoreException(String message){super(message);}
    public CorruptedStoreException(String message, Throwable cause){super(message, cause);}

}
