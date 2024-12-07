package tech.webclouds.simpledrivejava.errors;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@SuppressWarnings("unused")
@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = true)
public class CustomProblemException extends RuntimeException {

    // Getters
    private final String type;
    private final String title;
    private final String detail;
    private final int status;
    private final String instance;

    // Default Constructor
    public CustomProblemException() {
        this("about:blank", "An error occurred", "No additional details provided", 500, "unknown");
    }

    // Constructor with Status Only
    public CustomProblemException(int status) {
        this("about:blank", "An error occurred", "No additional details provided", status, "unknown");
    }

    // Constructor with Status and Detail
    public CustomProblemException(int status, String detail) {
        this("about:blank", "An error occurred", detail, status, "unknown");
    }

    // Constructor with Title, Status, and Detail
    public CustomProblemException(String title, int status, String detail) {
        this("about:blank", title, detail, status, "unknown");
    }

    // Full Constructor with All Fields
    public CustomProblemException(String type, String title, String detail, int status, String instance) {
        super(detail);
        this.type = type;
        this.title = title;
        this.detail = detail;
        this.status = status;
        this.instance = instance;
    }

}
