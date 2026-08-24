package RHIS.com.RHIS.core.exception;

public class RessourceNotFoundException extends RuntimeException {
    public RessourceNotFoundException(String resourceName,Object id) {
        super(resourceName +"not Found with id "+id);
    }
}
