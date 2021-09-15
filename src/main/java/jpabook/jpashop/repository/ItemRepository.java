package jpabook.jpashop.repository;

import jpabook.jpashop.domain.item.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ItemRepository {

    private final EntityManager em;

    /**
     * item 은 JPA 에 저장하기 전까지 id 값이 없다 (완전히 새로 생성된다)
     * em.persist(item); -> insert 와 비슷하다 (신규 등록)
     * em.merge() -> update 와 비슷하다 (갱신), 실무에서 쓸일이 없다.
     * <p>
     * em.merge(item) 으로 넘어온 파라미터(item)가 "준영속 -> 영속 상태"로 변하지 않는다.
     * em.merge(item) 가 영속상태의 값을 반환한다.
     * <p>
     * em.merge() 말고 변경 감지 기능을 사용해라
     */
    public void save(Item item) {
        if (item.getId() == null) {
            em.persist(item);
        } else {
            em.merge(item);
        }
    }

    public Item findOne(Long id) {
        return em.find(Item.class, id);
    }

    public List<Item> findAll() {
        return em.createQuery("select i from Item i", Item.class)
                .getResultList();
    }
}
