package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * xToOne(ManyToOne, OneToOne) 관계 최적화
 * Order, Order -> Member, Order -> Delivery
 */
@RestController
@RequiredArgsConstructor
public class OrderSimpleApiController {

    private final OrderRepository orderRepository;
    private final OrderSimpleQueryRepository orderSimpleQueryRepository;

    /**
     * DTO가 아닌 엔티티를 사용한 경우
     * Order와 Member가 양방향 연관관계를 이루고 있기 때문에
     * -> 무한 루프에 빠진다
     *
     * <p>
     * Order속 Member는 지연로딩으로 설정되어있다
     * -> Member는 프록시 객체가 된다
     * -> bytebuddy error가 발생한다.
     * -> Hibernate5Module 모듈을 bean으로 등록해서 억지로 해결할 수 있다.
     *
     * <p>
     * V1. 엔티티 직접 노출
     * - Hibernate5Module 모듈 등록, LAZY=null 처리 : 강제로 LAZY 로딩
     * - 양방향 관계 문제 발생 -> @JsonIgnore
     */
    @GetMapping("/api/v1/simple-orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            // LAZY 로딩된 Member 에 값을 채우기 위해
            // 강제 초기화
            order.getMember().getName();
            order.getMember().getAddress();
        }
        return all;
    }

    /**
     * V2. 엔티티를 조회해서 DTO로 변환(fetch join 사용X)
     * - 단점: 지연로딩으로 쿼리 N번 호출
     * <p>
     * List<SimpleOrderDto> 처럼 리스트를 바로 반환하는 것보다
     * -> Result<List<SimpleOrderDto>> 으로 한번 감싸서 반환해라 (JSON 확장을 위해서)
     *
     * <p>
     * 하지만 v2도 문제가 있다. (지연로딩에 의한 성능 이슈)
     * List<SimpleOrderDto> 를 반환하기까지
     * Order, Member, Delivery 3개의 테이블의 데이터를 가져와야한다.
     * Order 의 결과가 N 개 라면
     * Order : 조회 1번
     * Order -> Member : LAZY 로딩 조회 N번
     * Order -> Delivery : LAZY 로딩 조회 N번
     * 총 1 + N + N 번 실행된다.
     * (지연로딩은 영속성 컨텍스트에서 조회하므로, 이미 조회된 경우 쿼리를 생략한다.)
     */
    @GetMapping("/api/v2/simple-orders")
    public List<SimpleOrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());

        return orders.stream()
                .map(SimpleOrderDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 1 + N + N 문제를 해결하기 위해 "fetch join"을 사용한다.
     *
     * <p>
     * V3. 엔티티를 조회해서 DTO로 변환(fetch join 사용O)
     * - fetch join으로 쿼리 1번 호출 (Order, Member, Delivery 를 한 번에 가져온다.)
     * -> orderRepository 에서 JPQL을 사용하는 메소드를 생성
     * 참고: fetch join에 대한 자세한 내용은 JPA 기본편 참고(정말 중요함)
     */
    @GetMapping("/api/v3/simple-orders")
    public List<SimpleOrderDto> ordersV3() {
        List<Order> all = orderRepository.findAllWithMemberDelivery();
        return all.stream()
                .map(SimpleOrderDto::new)
                .collect(Collectors.toList());
    }

    @Data
    @RequiredArgsConstructor
    static class SimpleOrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate; //주문시간
        private OrderStatus orderStatus;
        private Address address;

        public SimpleOrderDto(Order order) {
            orderId = order.getId();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            name = order.getMember().getName(); // LAZY 초기화
            address = order.getDelivery().getAddress(); // LAZY 초기화
        }
    }

    /**
     * V3 에서는 엔티티를 DTO로 변환했지만
     * V4. JPA에서 DTO로 바로 조회한다.
     * - 쿼리 1번 호출
     * - select 절에서 "원하는 데이터만" 선택해서 조회
     *
     * <p>
     * V3보다 select 하는 필드가 적기 때문에
     * 네트워크 성능면에서 이득 (그렇게 차이나진 않지만..)
     * <p>
     * V3와 V4는 서로 장단점이 있다.
     * V3만으로도 대부분의 성능 이슈가 해결된다
     */
    @GetMapping("/api/v4/simple-orders")
    public List<OrderSimpleQueryDto> ordersV4() {
        return orderSimpleQueryRepository.findOrderDtos();
    }


    /*
     * 쿼리 방식 선택 권장 순서 정리
     * 1. 우선 엔티티를 DTO로 변환하는 방법을 선택한다.
     * 2. 필요하면 페치 조인으로 성능을 최적화 한다. 대부분의 성능 이슈가 해결된다.
     * 3. 그래도 안되면 DTO로 직접 조회하는 방법을 사용한다.
     * 4. 최후의 방법은 JPA가 제공하는 네이티브 SQL이나 스프링 JDBC Template을 사용해서 SQL을 직접
     * 사용한다.
     */

}
