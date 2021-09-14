package jpabook.jpashop.domain;

import lombok.Getter;

import javax.persistence.Embeddable;


@Embeddable
@Getter
public class Address {

    private String city;
    private String street;
    private String zipcode;

    /**
     * 값 타입은 기본 생성자가 필수다 (public/protected)
     * -> JPA 구현 라이브러리가 객체를 생성할 때 리플랙션/프록시 같은 기술을
     * 사용할 수 있도록 지원해야 하기 때문
     */
    public Address() {
    }

    /**
     * 값 타입은 데이터 변경이 불가능하게 설계해야 한다.
     * -> Setter 를 없애고 생성자로 값을 초기화하도록 한다.
     */
    public Address(String city, String street, String zipcode) {
        this.city = city;
        this.street = street;
        this.zipcode = zipcode;
    }


}
