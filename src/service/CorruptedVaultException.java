package service;

public class CorruptedVaultException extends VaultException{
    public CorruptedVaultException(String message){super(message);}
    public CorruptedVaultException(String message, Throwable cause){super(message, cause);}
}
