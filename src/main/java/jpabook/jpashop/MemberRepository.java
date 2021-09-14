package jpabook.jpashop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Repository
public class MemberRepository {

    @PersistenceContext
    private EntityManager em;

    public Long save(Member member) {
        em.persist(member);
        // 저장을 한 뒤 Id를 리턴해라
        return member.getId();
    }

    public Member find(Long id) {
        return em.find(Member.class, id);
    }
}
