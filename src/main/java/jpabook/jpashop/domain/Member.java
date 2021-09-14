package jpabook.jpashop.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 엔티티에는 가급적 Setter를 사용하지 말자
 * -> 나중에 리펙토링으로 @Setter 제거
 */
@Setter
@Getter
@Entity
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String username;

    @Embedded
    private Address address;

    /**
     * 연관관계의 주인이 아니라 거울이다
     *
     * @OneToMany(mappedBy = "member") : Order 엔티티의 member 변수에 매핑됨
     */
    @OneToMany(mappedBy = "member")
    private List<Order> orders = new ArrayList<>();

}
