package jpabook.jpashop.controller;

import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/members/new")
    public String createForm(Model model) {
        model.addAttribute("memberForm", new MemberForm());
        return "members/createMemberForm";
    }

    /**
     * @Valid : javax의 validation을 쓴다는 의미이다.
     * -> MemberForm에서 javax.validatation 어노테이션이 붙은 필드에 한하여 validate 한다
     * (@NotEmpty)
     * <p>
     * BindingResult
     * -> 오류가 담긴다
     * -> 담긴 오류로 분기처리 할 수 있다.
     * <p>
     * if (result.hasErrors()) {
     * return "members/createMemberForm";
     * }
     * -> MemberForm의 @NotEmpty(message = "회원 이름은 필수 입니다.")의 메시지가
     * createMemberForm.html 속에서 랜더링 과정을 거친 뒤
     * input box 아래에 보여진다.
     */
    @PostMapping("/members/new")
    public String create(@Valid MemberForm form, BindingResult result) {

        if (result.hasErrors()) {
            return "members/createMemberForm";
        }

        Address address = new Address(form.getCity(), form.getStreet(), form.getZipcode());

        Member member = new Member();
        member.setName(form.getName());
        member.setAddress(address);

        memberService.join(member);
        return "redirect:/";
    }

    /**
     * Member 엔티티보다 MemberForm 을 사용하는게 더 바람직하다.
     */
    @GetMapping("/members")
    public String list(Model model) {
        List<Member> members = memberService.findMembers();
        model.addAttribute("members", members);

        return "members/memberList";
    }
}
