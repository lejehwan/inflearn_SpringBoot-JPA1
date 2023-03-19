package jpabook.jpashop.service;

import jakarta.persistence.EntityManager;
import jpabook.jpashop.domain.Address;
import jpabook.jpashop.domain.Member;
import jpabook.jpashop.domain.Order;
import jpabook.jpashop.domain.OrderStatus;
import jpabook.jpashop.domain.item.Book;
import jpabook.jpashop.domain.item.Item;
import jpabook.jpashop.exception.NotEnoughStockException;
import jpabook.jpashop.repository.OrderRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional

public class OrderServiceTest {

    @Autowired
    EntityManager em;
    @Autowired
    OrderService orderService;
    @Autowired
    OrderRepository orderRepository;

    @Test
    public void 상품주문() throws Exception{
        //given
        Member member = createMember();
        Book book = createBook("시골 JPA", 10000, 10);
        int orderCount = 2;

        //when
        Long orderId = orderService.order(member.getId(), book.getId(), orderCount);

        //then
        Order getOrder = orderRepository.findOne(orderId);

        // 상품 주문시 상태는 ORDER
        assertThat(OrderStatus.ORDER).isEqualTo(getOrder.getStatus());
        // 주문한 상품 종류수가 정확해야 한다.
        assertThat(1).isEqualTo(getOrder.getOrderItems().size());
        // 주문 가격은 가격 * 수량 이다.
        assertThat(10000 * orderCount).isEqualTo(getOrder.getTotalPrice());
        // 주문 수량만큼 재고가 줄어야 한다.
        assertThat(8).isEqualTo(book.getStockQuantity());
    }

    @Test(expected = NotEnoughStockException.class)
    public void 상품주문_재고수량초과() throws Exception{
        //given
        Member member = createMember();
        Item item = createBook("시골 JPA", 10000, 10);

        int orderCount = 11;
        
        //when
        orderService.order(member.getId(), item.getId(), orderCount);

        //then
        assertThatThrownBy(() -> orderService.order(member.getId(), item.getId(), orderCount))
                .isInstanceOf(NotEnoughStockException.class)
                .hasMessageContaining("need more stock");
    }

    @Test
    public void 주문취소() throws Exception{
        //given
        Member member = createMember();
        Book item = createBook("시골 JPA", 10000, 10);
        int orderCount = 2;
        Long orderId = orderService.order(member.getId(), item.getId(), orderCount);

        //when
        orderService.cancelOrder(orderId);

        //then
        Order getOrder = orderRepository.findOne(orderId);
        // 주문 취소시 상태는 CANCEL 이다
        assertThat(OrderStatus.CANCEL).isEqualTo(getOrder.getStatus());
        // 주문이 취소된 상품은 그만큼 재고가 증가해야 한다.
        assertThat(10).isEqualTo(item.getStockQuantity());
    }

    @Test
    public void 상품재고수량초과() throws Exception{
        //given

        //when

        //then
    }

    private Book createBook(String name, int price, int stockQuantity) {
        Book book = new Book();
        book.setName(name);
        book.setPrice(price);
        book.setStockQuantity(stockQuantity);
        em.persist(book);
        return book;
    }

    private Member createMember() {
        Member member = new Member();
        member.setName("회원1");
        member.setAddress(new Address("서울", "강가", "123-123"));
        em.persist(member);
        return member;
    }
}