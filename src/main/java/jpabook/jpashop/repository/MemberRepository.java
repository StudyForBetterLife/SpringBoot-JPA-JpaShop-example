package jpabook.jpashop.repository;

import jpabook.jpashop.domain.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * @Component 하위의 @Repository
 * -> 스프링 부트 실행시 ComponentScan 하면서 자동으로 스프링 빈 등록됨
 */
@Repository
@RequiredArgsConstructor
public class MemberRepository {

    /**
     * @PersistenceContext -> 스프링이 EntityManager을 의존성 주입해준다.
     * Spring Data JPA 가 @Autowired 어노테이션으로 injection 되도록 지원해준다
     * -> 그럼 롬복의 @RequiredArgsConstructor 어노테이션을 활용할 수 있다.
     * @RequiredArgsConstructor private final EntityManager em;
     */
    private final EntityManager em;

    /**
     * EntityManagerFactory 는 @PersistenceUnit으로 빈 설정
     *
     * @PersistenceUnit private EntityManagerFactory emf;
     */

    public void save(Member member) {
        em.persist(member);
    }

    public Member findOne(Long id) {
        return em.find(Member.class, id);
    }

    /**
     * JPQL로 쿼리를 작성한다
     */
    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findByName(String name) {
        return em.createQuery("select m from Member m where m.name = :name", Member.class)
                .setParameter("name", name)
                .getResultList();
    }
}
