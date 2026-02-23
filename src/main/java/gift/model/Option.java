package gift.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Option {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int quantity;

    @ManyToOne
    private Product product;

    protected Option() {
    }

    public Option(final String name, final int quantity, final Product product) {
        this(null, name, quantity, product);
    }

    public Option(final Long id, final String name, final int quantity, final Product product) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.product = product;
    }

    public void decrease(final int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("차감 수량은 1 이상이어야 합니다");
        }
        if (this.quantity < quantity) {
            throw new IllegalStateException("재고가 부족합니다");
        }
        this.quantity -= quantity;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }

    public Product getProduct() {
        return product;
    }
}
