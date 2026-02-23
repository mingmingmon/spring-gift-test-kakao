package gift.application;

import gift.model.Category;
import gift.model.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(final CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Category create(final CreateCategoryRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("카테고리 이름은 필수입니다");
        }
        return categoryRepository.save(new Category(request.getName()));
    }

    public List<Category> retrieve() {
        return categoryRepository.findAll();
    }
}
