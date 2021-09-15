package jpabook.jpashop.exception;

public class NotEnoughStockException extends RuntimeException{

    /**
     * RuntimeException 의 메소드들을 Override 한다
     */
    public NotEnoughStockException() {
        super();
    }

    public NotEnoughStockException(String message) {
        super(message);
    }

    public NotEnoughStockException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotEnoughStockException(Throwable cause) {
        super(cause);
    }

}
