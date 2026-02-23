package gift.infrastructure;

import gift.model.Gift;
import gift.model.GiftDelivery;
import gift.model.Member;
import gift.model.MemberRepository;
import gift.model.Option;
import gift.model.Product;
import org.springframework.stereotype.Component;

@Component
class FakeGiftDelivery implements GiftDelivery {
    private final MemberRepository memberRepository;

    public FakeGiftDelivery(final MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public void deliver(final Gift gift) {
        final Member sender = memberRepository.findById(gift.getFrom()).orElseThrow();
        final Member receiver = memberRepository.findById(gift.getTo()).orElseThrow();
        final Option option = gift.getOption();
        final Product product = option.getProduct();
        System.out.println(sender.getName() + " → " + receiver.getName() + ": " + product.getName() + " " + option.getName());
    }
}
