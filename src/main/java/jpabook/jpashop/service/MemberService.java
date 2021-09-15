package jpabook.jpashop.service;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * JPA 에서 모든 데이터 변경/로직 들은 트랜잭션 안에서 이뤄져야 한다.
 * -> @Transactional 를 클래스 레벨에 사용하면 public 메소드들에 영향을 준다
 * - 스프링이 제공하는 @Transactional 어노테이션을 사용해야 좋다 (사용 가능한 옵션이 많다)
 */
@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
public class MemberService {

    /**
     * @Autowired 스프링 빈에 등록되어있는 MemberRepository 를 MemberService 로 의존성 주입한다
     * @Autowired private MemberRepository memberRepository;
     * <p>
     * 대신
     * <p>
     * ## "Setter Injection"
     * @Autowired public void setMemberRepository(MemberRepository memberRepository) {
     * this.memberRepository = memberRepository;
     * }
     * 와 같이 Setter Injection을 사용하면 (스프링이 memberRepository를 바로 주입하지 않고 Setter를 통해 주입한다)
     * -> 테스트 코드 작성시 Mock을 직접 주입할 수 있다.
     * (@Autowired private MemberRepository memberRepository; 의 경우는 테스트 코드에서 Mock을 주입하기 까다롭다)
     * -> 하지만 Setter를 사용하면 값 변경에 대한 위험성이 있다
     * <p>
     * ## "생성자 Injection"
     * private final MemberRepository memberRepository;
     * @Autowired public MemberService(MemberRepository memberRepository) {
     * this.memberRepository = memberRepository;
     * }
     * -> MemberRepository 가 생성될 때 injection이 완성되기 때문에 "Setter Injection" 보다 안정적이다.
     * 또한 테스트 케이스 작성시 생성자를 통해 Mock 을 직접 주입할 수 있도록 유도한다.
     * 요즘 스프링은 @Autowired 어노테이션 없이 자동으로 Injection 해준다
     * <p>
     * ## "Lombok 적용"
     * @RequiredArgsConstructor private final MemberRepository memberRepository;
     * -> 클래스 레벨에 해당 어노테이션을 달면
     * final 필드에 대해서만 생성자를 만들어준다
     */
    private final MemberRepository memberRepository;


    /**
     * 회원가입
     * em.persist(member) 하면 영속성 컨택스트속 1차 캐시의 pk필드에 id값이 저장된다
     * 그 id 값 (member.getId()) 을 return 한다
     * <p>
     * 클래스 레벨에서 @Transactional(readOnly = true) 이지만
     * 메소드 레벨의 @Transactional 이 우선권을 가진다 -> 읽기 전용이 아닌 읽기/쓰기 가능
     */
    @Transactional
    public Long join(Member member) {
        validateDuplicateMember(member); // 중복 회원 검증
        memberRepository.save(member);

        return member.getId();
    }

    /**
     * 동시에 같은 이름의 회원이 가입할 경우가 있다
     * -> 데이터베이스에서 Member의 name 속성에 unique 제약 조건을 설정해줘야 한다.
     */
    private void validateDuplicateMember(Member member) {
        // 중복 회원인 경우 예외
        List<Member> findMembers = memberRepository.findByName(member.getName());
        if (!findMembers.isEmpty()) {
            throw new IllegalStateException("이미 존재하는 회원입니다");
        }
    }

    /**
     * 회원 전체 조회
     *
     * @Transactional(readOnly = true) : 읽기 전용
     * -> JPA가 조회하는 곳에서 성능 최적화가 가능해진다
     * - 영속성 컨택스트를 flush 하지 않고, dirty checking 을 하지 않는다
     */
    // @Transactional(readOnly = true)
    public List<Member> findMembers() {
        return memberRepository.findAll();
    }

    /**
     * 회원 한명 조회
     */
    // @Transactional(readOnly = true)
    public Member findOne(Long memberId) {
        return memberRepository.findOne(memberId);
    }

    @Transactional
    public void update(Long id, String name) {
        Member member = memberRepository.findOne(id);
        member.setName(name);
    }
}
