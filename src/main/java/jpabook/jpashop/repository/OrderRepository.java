package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.repository.order.simplequery.OrderSimpleQueryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;

    /**
     * 주문 로직
     */
    public void save(Order order) {
        em.persist(order);
    }

    public Order findOne(Long id) {
        return em.find(Order.class, id);
    }

    /**
     * 동적 쿼리..
     * -> Querydsl 로 하자
     */
    public List<Order> findAll(OrderSearch searchOrder) {
        return em.createQuery("select o from Order o join o.member m" +
                        " where o.status = :status " +
                        " and m.name like :name", Order.class)
                .setParameter("status", searchOrder.getOrderStatus())
                .setParameter("name", searchOrder.getMemberName())
                .setMaxResults(1000) // 최대 1000 개
                .getResultList();
    }

    // Criteria -> 이것도 비추
    public List<Order> findAllByCriteria(OrderSearch orderSearch) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> o = cq.from(Order.class);
        Join<Order, Member> m = o.join("member", JoinType.INNER); //회원과 조인
        List<Predicate> criteria = new ArrayList<>();
        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            Predicate status = cb.equal(o.get("status"),
                    orderSearch.getOrderStatus());
            criteria.add(status);
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            Predicate name =
                    cb.like(m.<String>get("name"), "%" +
                            orderSearch.getMemberName() + "%");
            criteria.add(name);
        }
        cq.where(cb.and(criteria.toArray(new Predicate[criteria.size()])));
        TypedQuery<Order> query = em.createQuery(cq).setMaxResults(1000); //최대 1000건
        return query.getResultList();
    }

    // 진짜 비추
    public List<Order> findAllByString(OrderSearch orderSearch) {
        //language=JPAQL
        String jpql = "select o From Order o join o.member m";
        boolean isFirstCondition = true;
        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }
        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.name like :name";
        }
        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
                .setMaxResults(1000); //최대 1000건
        if (orderSearch.getOrderStatus() != null) {
            query = query.setParameter("status", orderSearch.getOrderStatus());
        }
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }
        return query.getResultList();
    }

    /**
     * V3
     * fetch join을 진짜 적극적으로 활용하자!!!!!
     *
     * <p>
     * fetch join (fetch는 JPA에만 있는 문법)
     * -> Order, Member, Delivery 를 조인해서 한번에 가져온다.
     * Order의 지연로딩 설정된 필드에 값을 채워서 가져온다. (프록시 ㄴㄴ, 찐 값 ㅇㅇ)
     */
    public List<Order> findAllWithMemberDelivery() {
        return em.createQuery(
                        "select o from Order o " +
                                " join fetch o.member m" +
                                " join fetch o.delivery d", Order.class)
                .getResultList();
    }

    /**
     * V3 엔티티를 DTO로 변환 - 페치 조인 최적화
     * <p>
     * [select o from Order o ..]
     * Order -> OrderItem 을 조인한다.
     * 데이터가 뻥튀기 된다.
     * <p>
     * [select distinct o from Order o ..]
     * -> "distinct" 키워드를 사용하여 중복된 데이터를 걸러줘야 한다.
     * JPA에서 distinct 키워드는
     * 1. sql의 distinct 기능도 하면서
     * 2. 엔티티의 id값이 같은 데이터를 걸러 컬랙션에 담는다. (Order객체의 id값이 같은 데이터를 중복제거 해준다)
     * <p>
     * 꼭 기억할 것
     * 1. 컬렉션 페치 조인을 사용하면 페이징이 불가능하다 (어마어마한 단점)
     * 하이버네이트는 경고 로그를 남기면서 모든
     * 데이터를 DB에서 읽어오고 (limit offset 이 없다. 즉, 모든 데이터를 다 가져온다는 의미),
     * 메모리에서 페이징 해버린다 (매우 위험하다).
     * 2. 컬렉션 둘 이상에 페치 조인을 사용하면 안된다. 데이터가
     * 부정합하게 조회될 수 있다
     */
    public List<Order> findAllWithItem() {
        return em.createQuery(
                        "select distinct o from Order o " +
                                " join fetch o.member m " +
                                " join fetch o.delivery d " +
                                " join fetch o.orderItems oi " +
                                " join fetch oi.item i", Order.class)
//                .setFirstResult(0)
//                .setMaxResults(100) // -> 경고를 내고 메모리에 데이터를 올려둔 다음 페이징한다. (큰일남)
                .getResultList();
    }

    /**
     * V3.1 엔티티를 DTO로 변환 - 페이징과 한계 돌파
     * <p>
     * "IN" 쿼리를 사용하여 데이터를 한번에 가져온다.
     * <p>
     * " join fetch o.member m" +
     * " join fetch o.delivery d"
     * 이 부분을 제거해도 batch_size:100 에 의해 최적화가 된다.
     * 하지만 네트워크를 많이 탄다.
     */
    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        return em.createQuery(
                        "select o from Order o " +
                                " join fetch o.member m" +
                                " join fetch o.delivery d", Order.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }
}
