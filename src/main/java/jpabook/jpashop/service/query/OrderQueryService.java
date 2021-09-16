package jpabook.jpashop.service.query;

import jpabook.jpashop.repository.order.query.OrderQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderQueryService {



}

*/

/**
 * OrderService 에서 트랜잭션을 타는 부분을 모두 OrderQueryService에 작성한뒤
 * OrderService 에서 private final OrderQueryService 를 호출하여 사용한다.
 *
 * <p>
 * 왜?
 * => OSVI를 끄면 지연 로딩 코드를 트랜잭션 안으로 넣어야 하기 때문이다.
 *
 * <p>
 * 왜 OSVI를 꺼야 하는 거야?
 * => 너무 오랜 시간동안 데이터베이스 커넥션 리소스를 사용하기 때문에 장애로 이어지기 때문이다.
 */