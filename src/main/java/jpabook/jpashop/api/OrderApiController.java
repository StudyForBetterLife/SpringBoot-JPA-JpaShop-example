package jpabook.jpashop.api;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderItem;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.repository.OrderRepository;
import jpabook.jpashop.repository.OrderSearch;
import jpabook.jpashop.repository.order.query.OrderFlatDto;
import jpabook.jpashop.repository.order.query.OrderItemQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryDto;
import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


/**
 * V1. 엔티티 직접 노출
 * - 엔티티가 변하면 API 스펙이 변한다.
 * - 트랜잭션 안에서 지연 로딩 필요
 * - 양방향 연관관계 문제
 * <p>
 * V2. 엔티티를 조회해서 DTO로 변환(fetch join 사용X)
 * - 트랜잭션 안에서 지연 로딩 필요
 * V3. 엔티티를 조회해서 DTO로 변환(fetch join 사용O)
 * - 페이징 시에는 N 부분을 포기해야함(대신에 batch fetch size? 옵션 주면 N -> 1 쿼리로 변경
 * 가능)
 * <p>
 * V4. JPA에서 DTO로 바로 조회, 컬렉션 N 조회 (1 + N Query)
 * - 페이징 가능
 * V5. JPA에서 DTO로 바로 조회, 컬렉션 1 조회 최적화 버전 (1 + 1 Query)
 * - 페이징 가능
 * V6. JPA에서 DTO로 바로 조회, 플랫 데이터(1Query) (1 Query)
 * - 페이징 불가능...
 */
@RestController
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderRepository orderRepository;
    private final OrderQueryRepository orderQueryRepository;

    /**
     * V1. 엔티티 직접 노출
     * - Hibernate5Module 모듈 등록, LAZY=null 처리
     * - 양방향 관계 문제 발생 -> @JsonIgnore
     * <p>
     * 의미 없이 데이터를 한번씩 건들여 LAZY 세팅된 필드에 값을 채워준다
     */
    @GetMapping("/api/v1/orders")
    public List<Order> ordersV1() {
        List<Order> all = orderRepository.findAllByString(new OrderSearch());
        for (Order order : all) {
            order.getMember().getName(); // -> Order의 Member : LAZY 강제 초기화
            order.getDelivery().getAddress();// -> Order의 Delivery : LAZY 강제 초기화
            List<OrderItem> orderItems = order.getOrderItems(); // -> Order의 OrderItem : LAZY 강제 초기화
            orderItems.forEach(o -> o.getItem().getName());  // -> OrderItem의 Item : LAZY 강제 초기화
        }

        return all;
    }

    /**
     * V2: 엔티티를 DTO로 변환
     * <p>
     * -> 지연 로딩 설정된 필드만큼 쿼리가 나간다
     * <p>
     * SQL 실행 수
     * order 1번
     * member , address N번(order 조회 수 만큼)
     * orderItem N번(order 조회 수 만큼)
     * item N번(orderItem 조회 수 만큼)
     * -> 컬랙션으로 fetch join 하여 최적화 해야한다.
     */
    @GetMapping("/api/v2/orders")
    public List<OrderDto> ordersV2() {
        List<Order> orders = orderRepository.findAllByString(new OrderSearch());
        List<OrderDto> collect = orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
        return collect;
    }

    /**
     * private List<OrderItem> orderItems;
     * -> DTO가 아닌 엔티티 OrderItem 을 사용하면 안된다.
     * -> private List<OrderItemDto> orderItems;
     */
    @Data
    static class OrderDto {
        private Long orderId;
        private String name;
        private LocalDateTime orderDate; //주문시간
        private OrderStatus orderStatus;
        private Address address;
        private List<OrderItemDto> orderItems;

        public OrderDto(Order order) {
            orderId = order.getId();
            name = order.getMember().getName();
            orderDate = order.getOrderDate();
            orderStatus = order.getStatus();
            address = order.getDelivery().getAddress();
            orderItems = order.getOrderItems().stream()
                    .map(OrderItemDto::new)
                    .collect(Collectors.toList());
        }
    }

    @Data
    static class OrderItemDto {
        private String itemName;//상품 명
        private int orderPrice; //주문 가격
        private int count; //주문 수량

        public OrderItemDto(OrderItem orderItem) {
            itemName = orderItem.getItem().getName();
            orderPrice = orderItem.getOrderPrice();
            count = orderItem.getCount();
        }
    }

    /**
     * V3 엔티티를 DTO로 변환 - 페치 조인 최적화
     * 페치 조인으로 SQL이 1번만 실행된다.
     * <p>
     * 단점 : 최적화된 페이징 불가능
     * => Order를 기준으로 페이징 하고 싶은데, 다(N)인 OrderItem을 조인하면 OrderItem이 기준이
     * 되어버린다
     */
    @GetMapping("/api/v3/orders")
    public List<OrderDto> ordersV3() {
        List<Order> orders = orderRepository.findAllWithItem();
        return orders.stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
    }

    /**
     * 꼭꼭꼭!!!!
     * <p>
     * V3.1 엔티티를 조회해서 DTO로 변환 페이징 고려
     * - 먼저 XToOne(OneToOne, ManyToOne) 관계를 모두 페치조인 한다
     * ==> ToOne 관계는 row수를 증가시키지 않으므로 페이징 쿼리에 영향을 주지 않는다.
     * ==> Order에서 Member, Delivery는 XToOne 관계이므로 이 둘은 jpql 에서 fetch join 한다.
     * ==> OrderItems 는 fetch join 하지 않는다!!
     * <p>
     * - 컬렉션은 지연 로딩으로 조회한다
     * ==> OrderItems는 LAZY 설정
     * <p>
     * - 지연 로딩 성능 최적화를 위해 컬렉션 관계는 hibernate.default_batch_fetch_size, @BatchSize로 최적화
     * => [글로벌 설정]
     * application.yml 파일에서
     * spring:jpa:properties:hibernate:default_batch_fetch_size:100 설정
     * 이 옵션을 사용하면 컬렉션이나, 프록시 객체를 한꺼번에 설정한 size 만큼 IN 쿼리로 조회한다
     * 무슨 말이냐면,
     * "where orderitems0_.order_id in (?, ?, ..)" : default_batch_fetch_size 는 (?, ?, ..)의 개수를 의미한다.
     * 미리 데이터를 in 쿼리를 통해 가져온다.
     * => [개별 최적화]
     * 개별로 설정하려면 @BatchSize 를 적용하면 된다. (컬렉션은 컬렉션 필드에, 엔티티는 엔티티 클래스에 적용)
     * <p>
     * [결론]
     * 1. ToOne 관계는 페치 조인해도 페이징에 영향을 주지 않는다.
     * 따라서 ToOne 관계는 페치조인으로 쿼리 수를 줄이고 해결하고,
     * 2. 나머지는 hibernate.default_batch_fetch_size 로 최적화 하자
     * 100~1000 사이를 선택하는 것을 권장한다
     * (데이터베이스에 따라 IN 절 파라미터를 1000으로 제한하기도 한다)
     * - 1000으로 잡으면 한번에 1000개를 DB에서 애플리케이션에 불러오므로 DB
     * 에 순간 부하가 증가할 수 있다
     * - 애플리케이션은 100이든 1000이든 결국 전체 데이터를 로딩해야
     * 하므로 메모리 사용량이 같다
     * => 결국 DB든 애플리케이션이든 순간 부하를 어디까지 견딜 수 있는지로 결정하면 된다.
     */
    @GetMapping("/api/v3.1/orders")
    public List<OrderDto> ordersV3_page(
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            @RequestParam(value = "limit", defaultValue = "100") int limit
    ) {
        List<Order> orders = orderRepository.findAllWithMemberDelivery(offset, limit);

        List<OrderDto> result = orders.stream()
                .map(o -> new OrderDto(o))
                .collect(Collectors.toList());

        return result;
    }

    /**
     * V4: JPA에서 DTO 직접 조회
     * <p>
     * OrderQueryRepository의
     * public List<OrderQueryDto> findOrderQueryDtos() 를 보면
     * Query: 루트 1번, 컬렉션 N 번 실행
     * ToOne(N:1, 1:1) 관계들을 먼저 조회하고, ToMany(1:N) 관계는 각각 별도로 처리한다
     * 1. ToOne 관계는 조인해도 데이터 row 수가 증가하지 않는다.
     * 먼저 ToOne 관계를 조인해서 데이터를 가져오고 (public List<OrderQueryDto> findOrders())
     * <p>
     * 2. ToMany(1:N) 관계는 조인하면 row 수가 증가한다.
     * findOrders()로 가져온 결과 속 OrderItems 들을 findOrderItems(o.getOrderId())의 결과로 채운다
     * (추가 쿼리가 나가는 문제점이 있다)
     */
    @GetMapping("/api/v4/orders")
    public List<OrderQueryDto> ordersV4() {
        return orderQueryRepository.findOrderQueryDtos();
    }

    /**
     * V5: JPA에서 DTO 직접 조회 - 컬렉션 조회 최적화
     * <p>
     * - Query: 루트 1번, 컬렉션 1번
     * - ToOne 관계들을 먼저 조회하고, 여기서 얻은 식별자 orderId로 ToMany 관계인 OrderItem 을
     * 한꺼번에 조회
     * -MAP을 사용해서 매칭 성능 향상(O(1))
     */
    @GetMapping("/api/v5/orders")
    public List<OrderQueryDto> ordersV5() {
        return orderQueryRepository.findAllByDto_optimization();
    }

    /**
     * V6: JPA에서 DTO로 직접 조회, 플랫 데이터 최적화
     * Query: 1번만 나간다
     * <p>
     * 단점
     * - 쿼리는 한번이지만 조인으로 인해 DB에서 애플리케이션에 전달하는 데이터에 중복 데이터가
     * - 추가되므로 상황에 따라 V5 보다 더 느릴 수 도 있다.
     * - 애플리케이션에서 추가 작업이 크다. (직접 중복제거 해줘야한다.)
     * - Order 를 기준으로 페이징 불가능 (OrderItem을 기준으로 페이징은 할 수 있다)
     */
    @GetMapping("/api/v6/orders")
    public List<OrderQueryDto> ordersV6() {
        List<OrderFlatDto> flats = orderQueryRepository.findAllByDto_flat();

        // 중복된 데이터인 flats 을 스트림을 활용하여 중복제거한다.
        // 개 복잡 ㅎㄷㄷ
        return flats.stream()
                .collect(Collectors.groupingBy(o -> new OrderQueryDto(o.getOrderId(), o.getName(), o.getOrderDate(), o.getOrderStatus(), o.getAddress()),
                        Collectors.mapping(o -> new OrderItemQueryDto(o.getOrderId(), o.getItemName(), o.getOrderPrice(), o.getCount()), Collectors.toList())
                )).entrySet().stream()
                .map(e -> new OrderQueryDto(e.getKey().getOrderId(),
                        e.getKey().getName(), e.getKey().getOrderDate(), e.getKey().getOrderStatus(),
                        e.getKey().getAddress(), e.getValue()))
                .collect(Collectors.toList());
    }
}
/**
 * [정리]
 * 엔티티 조회
 * - 엔티티를 조회해서 그대로 반환: V1
 * - 엔티티 조회 후 DTO로 변환: V2
 * - 페치 조인으로 쿼리 수 최적화: V3
 * - 컬렉션 페이징과 한계 돌파: V3.1
 * -- 컬렉션은 페치 조인시 페이징이 불가능
 * -- ToOne 관계는 페치 조인으로 쿼리 수 최적화
 * -- 컬렉션은 페치 조인 대신에 지연 로딩을 유지하고, hibernate.default_batch_fetch_size ,@BatchSize 로 최적화
 * DTO 직접 조회
 * -JPA에서 DTO를 직접 조회: V4
 * -컬렉션 조회 최적화 - 일대다 관계인 컬렉션은 IN 절을 활용해서 메모리에 미리 조회해서 최적화: V5
 * -플랫 데이터 최적화 - JOIN 결과를 그대로 조회 후 애플리케이션에서 원하는 모양으로 직접 변환: V6
 *
 * <p>
 * [권장 순서]
 * 1. 엔티티 조회 방식으로 우선 접근 (추천) (JPA -> 엔티티 -> DTO)
 * - 1. 페치조인으로 쿼리 수를 최적화
 * - 2. 컬렉션 최적화
 * -- 1. 페이징 필요 hibernate.default_batch_fetch_size , @BatchSize 로 최적화
 * -- 2. 페이징 필요X 페치 조인 사용
 * 2. 엔티티 조회 방식으로 해결이 안되면 DTO 조회 방식 사용 (JPA -> DTO)
 * 3. DTO 조회 방식으로 해결이 안되면 NativeSQL or 스프링 JdbcTemplate
 *
 * <p>
 * [DTO 조회 방식의 선택지]
 * => DTO로 조회하는 방법도 각각 장단이 있다. V4, V5, V6에서 단순하게 쿼리가 1번 실행된다고 V6이 항상
 * 좋은 방법인 것은 아니다.
 * => V4는 코드가 단순하다. 특정 주문 한건만 조회하면 이 방식을 사용해도 성능이 잘 나온다. 예를 들어서
 * 조회한 Order 데이터가 1건이면 OrderItem을 찾기 위한 쿼리도 1번만 실행하면 된다.
 * => V5는 코드가 복잡하다. 여러 주문을 한꺼번에 조회하는 경우에는 V4 대신에 이것을 최적화한 V5 방식을
 * 사용해야 한다. 예를 들어서 조회한 Order 데이터가 1000건인데, V4 방식을 그대로 사용하면, 쿼리가 총
 * 1 + 1000번 실행된다. 여기서 1은 Order 를 조회한 쿼리고, 1000은 조회된 Order의 row 수다. V5
 * 방식으로 최적화 하면 쿼리가 총 1 + 1번만 실행된다. 상황에 따라 다르겠지만 운영 환경에서 100배
 * 이상의 성능 차이가 날 수 있다.
 * => V6는 완전히 다른 접근방식이다. 쿼리 한번으로 최적화 되어서 상당히 좋아보이지만, Order를 기준으로
 * 페이징이 불가능하다. 실무에서는 이정도 데이터면 수백이나, 수천건 단위로 페이징 처리가 꼭 필요하므로,
 * 이 경우 선택하기 어려운 방법이다. 그리고 데이터가 많으면 중복 전송이 증가해서 V5와 비교해서 성능
 * 차이도 미비하다.
 */