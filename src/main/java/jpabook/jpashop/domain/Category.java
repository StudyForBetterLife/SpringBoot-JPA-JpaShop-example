package jpabook.jpashop.domain;

import jpabook.jpashop.domain.item.Item;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
public class Category {

    @Id
    @GeneratedValue
    @Column(name = "category_id")
    private Long id;

    private String name;

    /**
     * Item & Category 는 실무에서 절대 사용하면 안 될 다대다 연관관계이다
     * 이 연관관계의 주인을 Category 로 설정하였다.
     * Item 과 Category 의 중간 테이블을 설정하기 위해 @JoinTable 어노테이션을 활용했다.
     * - name = "category_item" : 중간 테이블의 이름
     * - joinColumns = @JoinColumn(name = "category_id") : 연관관계의 주인의 pk를 중간테이블의 fk로
     * - inverseJoinColumns = @JoinColumn(name = "item_id") : 반대편의 pk를 중간테이블의 fk로
     */
    @ManyToMany
    @JoinTable(
            name = "category_item",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "item_id")
    )
    private List<Item> items = new ArrayList<>();


    /**
     * Self 양방향 연관관계이다
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent")
    private List<Category> child = new ArrayList<>();

    /**
     * 연관관계 편의 메소드
     * -> addChildCategory
     **/
    public void addChildCategory(Category child) {
        this.child.add(child);
        child.setParent(this);
    }

}
