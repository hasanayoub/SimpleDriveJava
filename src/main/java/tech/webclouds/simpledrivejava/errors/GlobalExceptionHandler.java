package tech.webclouds.simpledrivejava.errors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomProblemException.class)
    public ResponseEntity<Map<String, Object>> handleCustomProblemException(CustomProblemException ex) {
        Map<String, Object> problemDetails = Map.of(
                "type", ex.getType(),
                "title", ex.getTitle(),
                "detail", ex.getDetail(),
                "status", ex.getStatus(),
                "instance", ex.getInstance()
        );

        return ResponseEntity.status(ex.getStatus()).body(problemDetails);
    }
}
