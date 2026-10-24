package com.orderflow.order.repository;

import com.orderflow.order.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ProductRepositoryTest {

    @Autowired ProductRepository repo;

    private Product save(String name, String price, boolean active) {
        Product p = new Product(name, new BigDecimal(price));
        p.setActive(active);
        return repo.save(p);
    }

    @Test
    void findAllByActiveTrueOrderByNameAsc_returnsOnlyActive_sortedByName() {
        save("Teclado", "699.00", true);
        save("Mouse", "549.90", true);
        save("Descontinuado", "10.00", false);
        save("Monitor", "1299.00", true);

        List<Product> result = repo.findAllByActiveTrueOrderByNameAsc();

        assertThat(result).extracting(Product::getName).containsExactly("Monitor", "Mouse", "Teclado");
    }

    @Test
    void findByIdAndActiveTrue_excludesInactive() {
        Product active = save("Mouse", "549.90", true);
        Product inactive = save("Descontinuado", "10.00", false);

        assertThat(repo.findByIdAndActiveTrue(active.getId())).isPresent();
        assertThat(repo.findByIdAndActiveTrue(inactive.getId())).isEmpty();
        assertThat(repo.findByIdAndActiveTrue(99999L)).isEmpty();
    }

    @Test
    void findAllByIdInAndActiveTrue_excludesInactiveAndMissing() {
        Product a = save("Mouse", "549.90", true);
        Product b = save("Descontinuado", "10.00", false);

        List<Product> result = repo.findAllByIdInAndActiveTrue(List.of(a.getId(), b.getId(), 99999L));

        assertThat(result).extracting(Product::getId).containsExactly(a.getId());
    }
}
