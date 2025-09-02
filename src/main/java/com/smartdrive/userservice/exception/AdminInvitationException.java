package com.smartdrive.userservice.exception;

/**
 * Exception thrown when admin invitation operations fail
 */
public class AdminInvitationException extends RuntimeException {
    
    public AdminInvitationException(String message) {
        super(message);
    }
    
    public AdminInvitationException(String message, Throwable cause) {
        super(message, cause);
    }
}
