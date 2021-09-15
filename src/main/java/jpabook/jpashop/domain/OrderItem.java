package jpabook.jpashop.domain;

import jpabook.jpashop.domain.item.Item;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

/**
 * protected OrderItem() {
 * }
 * -> new OrderItem()으로 생성을 막고, createOrderItem()으로 생성하도록 유도할 수 있다
 *
 * @NoArgsConstructor(access = AccessLevel.PROTECTED)
 * 어노테이션을 통해 protected 생성자를 생략할 수 있다.
 */
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Setter
@Getter
@Entity
public class OrderItem {

    @Id
    @GeneratedValue
    @Column(name = "order_item_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    private int orderPrice; // 주문 당시 가격
    private int count; // 주문 당시 수량

    /**
     * 생성 메소드
     */


    public static OrderItem createOrderItem(Item item, int orderPrice, int count) {
        OrderItem orderItem = new OrderItem();
        orderItem.setItem(item);
        orderItem.setOrderPrice(orderPrice);
        orderItem.setCount(count);

        // OrderItem 을 생성하면서 재고를 까준다
        item.removeStock(count);
        return orderItem;
    }

    /**
     * 비지니스 로직
     * 주문 취소
     */
    public void cancel() {
        // 주문 당시 수량 만큼 다시 재고를 올려준다
        getItem().addStock(count);
    }

    /**
     * 조회 로직
     * 주문 상품 전체 가격 조회
     */
    public int getTotalPrice() {
        return getOrderPrice() * getCount();
    }
}
