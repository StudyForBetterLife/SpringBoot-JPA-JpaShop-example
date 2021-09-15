package jpabook.jpashop.service;

import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional
    public void saveItem(Item item) {
        itemRepository.save(item);
    }

    /**
     * updateItem() : 준영속 엔티티를 수정하는 2가지 방법 중 1번 방법
     * -> 1. 변경 감지 기능 사용 (em.merge() 보다 더 나은 방법 : 원하는 속성만 선택해서 변경할 수 있다.)
     * <p>
     * 2. em.merge() 방법은 updateItem 메소드와 완전히 동일하다
     * -> merge의 위험성 : 만약 merge의 파라미터에 값이 없는 필드는 null로 업데이트 될 위험이 있다.
     * <p>
     * 가급적 em.merge()를 사용하지 말고 변경 감지 기능으로 한땀 한땀 변경하는게 안전하다
     */
    @Transactional
    public Item updateItem(Long itemId, String name, int price, int stockQuantity) {
        Item findItem = itemRepository.findOne(itemId);
        // setter 가 아니라
        // 의미있는 메소드 (예를 들어 findItem.change(price, name, stockQuantity);)
        // 로 변경 감지 기능을 구현하는게 좋다
        findItem.setName(name);
        findItem.setPrice(price);
        findItem.setStockQuantity(stockQuantity);
        return findItem;
    }

    public List<Item> findItems() {
        return itemRepository.findAll();
    }

    public Item findOne(Long id) {
        return itemRepository.findOne(id);
    }
}
