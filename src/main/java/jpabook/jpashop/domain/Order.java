package jpabook.jpashop.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue
    @Column(name = "order_id")
    private Long id;

    /**
     * fk를 가진 엔티티가 연관관계의 주인이 되어야 한다
     * -> Order가 Member & Order의 연관관계 주인이다.
     *
     * @XToOne 은 무조건 지연로딩으로 설정해줘야한다.
     * -> @XToOne(fetch = FetchType.LAZY)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id") // fk의 이름이 member_id
    private Member member;

    /**
     * cascade = CascadeType.ALL
     * -> Order의 orderItems에 데이터를 넣어 두고
     * em.persist(order); 만 해두면
     * orderItems 도 같이 영속성 컨택스트에 저장된다.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems = new ArrayList<>();

    /**
     * Order & Delivery는 일대일 연관관계를 가진다
     * 일대일 연관관계에서는 주인을 어디에 설정하던 상관 없다.
     * 연관관계의 주인을 Order로 설정한다.
     * -> @JoinColumn(name = "delivery_id")
     *
     * @OneToX 는 기본이 지연로딩이다.
     * -> @OneToOne(fetch = FetchType.LAZY) == @OneToOne
     */
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "delivery_id")
    private Delivery delivery;

    private LocalDateTime orderDate;

    private OrderStatus orderStatus; // 주문 상태

    /**
     * 연관관계 편의 메소드들 (연관관계 주인 쪽에 둬야한다)
     * -> setMember, addOrderItem, setDelivery
     */
    public void setMember(Member member) {
        this.member = member;
        member.getOrders().add(this);
    }

    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
        delivery.setOrder(this);
    }
}
