package io.github.lukwalczak1.framework.web.response;

public class ResponseEntity<T> {
    private final T body;
    private final int statusCode;

    public ResponseEntity(int statusCode, T body) {
        this.body = body;
        this.statusCode = statusCode;
    }

    public static <T> ResponseEntity<T> ok(T body) {
        return new ResponseEntity<>(200, body);
    }

    public static <T> ResponseEntity<T> response(int statusCode, T body) {
        return new ResponseEntity<>(statusCode, body);
    }

    public static <T> ResponseEntity<T> ok() {
        return new ResponseEntity<>(200, null);
    }

    public T getBody() {
        return body;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
