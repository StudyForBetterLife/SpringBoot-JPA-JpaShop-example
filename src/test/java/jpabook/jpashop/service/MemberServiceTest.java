package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @RunWith(SpringRunner.class)
 * @SpringBootTest -> 두 어노테이션으로 테스트 시점에 스프링 부트를 올려 실행할 수 있다
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional
public class MemberServiceTest {

    /**
     * @SpringBootTest 어노테이션 덕분에
     * @Autowired 어노테이션을 사용할 수 있다.
     */
    @Autowired
    MemberService memberService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    EntityManager em;

    /**
     * @Transactional 는 테스트 수행 완료 후 DB를 Rollback 한다
     * -> 메소드 레벨의 @Rollback(value = false) 으로 데이터 rollback 을 막을 수 있다
     * -> 또는 em.flush() 을 통해 insert 쿼리를 볼 수 있다.
     */
    @Test
    public void 회원가입() throws Exception {
        // given
        Member member = new Member();
        member.setName("kim");

        // when
        Long savedId = memberService.join(member);

        // then
        em.flush();
        Assert.assertEquals(member, memberRepository.findOne(savedId));
    }

    /**
     * @Test(expected = IllegalStateException.class) 를 달아주면
     * <p>
     * try {
     * memberService.join(member2); // 예외가 발생해야 한다!! -> throw 됨
     * } catch (IllegalStateException e) {
     * return;
     * }
     * <p>
     * 위와 같은 try catch 구문 없이 작성할 수 있다
     * <p>
     * memberService.join(member1);
     * memberService.join(member2); // 예외가 발생해야 한다!! -> throw 됨
     */
    @Test(expected = IllegalStateException.class)
    public void 중복_회원_예외() throws Exception {
        // given
        Member member1 = new Member();
        member1.setName("kim");

        Member member2 = new Member();
        member2.setName("kim");

        // when
        memberService.join(member1);
        memberService.join(member2); // 예외가 발생해야 한다!! -> throw 됨

        // then
        fail("예외가 발생해야 한다.");
    }
}