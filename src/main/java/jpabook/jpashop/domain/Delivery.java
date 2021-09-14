package jpabook.jpashop.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Setter
@Getter
@Entity
public class Delivery {

    @Id
    @GeneratedValue
    @Column(name = "delivery_id")
    private Long id;

    /**
     * 일대일 연관관계의 주인을 Delivery가 아닌 Order로 설정했기 때문에
     * mappedBy = "delivery" 해준다.
     */
    @OneToOne(mappedBy = "delivery", fetch = FetchType.LAZY)
    private Order order;

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING) // 무조건 STRING 으로
    private DeliveryStatus status;
}
