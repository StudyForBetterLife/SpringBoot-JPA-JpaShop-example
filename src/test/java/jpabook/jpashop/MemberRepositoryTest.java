package jpabook.jpashop;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class) // JUnit에게 스프링과 관련된 테스트를 한다고 알린다.
@SpringBootTest
public class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;


    /*
     *  @Transactional
     *  - javax 가 아닌 spring framework 것을 사용해라
     *  - @Test 와 같이 사용되면 자동적으로 DB rollback 된다.
     *  - @Rollback(value = false) 으로 디비가 롤백되는 것을 막을 수 있다.
     * */

    @Test
    @Transactional
    @Rollback(value = false)
    public void testMember() throws Exception {
        // given
        Member member = new Member();
        member.setUsername("memberA");

        // when
        Long saveId = memberRepository.save(member);
        Member findMember = memberRepository.find(saveId);

        // then
        Assertions.assertThat(findMember.getId()).isEqualTo(member.getId());
        Assertions.assertThat(findMember.getUsername()).isEqualTo(member.getUsername());

        /*
         * 같은 트랜잭션 안에서 영속성 컨택스트도 동일하다.
         * 같은 영속성 컨택스트 안에서 id값 (식별자) 가 같으면 == 비교시 true 이다.
         * 영속성 컨택스트의 1차 캐시에서 값을 가져오기 때문에 쿼리조차 나가지 않는다.
         * */
        Assertions.assertThat(findMember).isEqualTo(member);
    }
}