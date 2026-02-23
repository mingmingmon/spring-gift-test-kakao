package gift.application;

import gift.model.Category;
import gift.model.CategoryRepository;
import gift.model.Product;
import gift.model.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(final ProductRepository productRepository, final CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Product create(final CreateProductRequest request) {
        if (request.getPrice() <= 0) {
            throw new IllegalArgumentException("상품 가격은 1 이상이어야 합니다");
        }
        final Category category = categoryRepository.findById(request.getCategoryId()).orElseThrow();
        final Product product = new Product(request.getName(), request.getPrice(), request.getImageUrl(), category);
        return productRepository.save(product);
    }

    public List<Product> retrieve() {
        return productRepository.findAll();
    }
}
