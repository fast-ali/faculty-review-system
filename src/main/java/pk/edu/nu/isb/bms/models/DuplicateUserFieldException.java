package pk.edu.nu.isb.bms.models;

public class DuplicateUserFieldException extends RuntimeException {
    public DuplicateUserFieldException(String message) {
        super(message);
    }
}
