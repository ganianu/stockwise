
package com.stockwise.repository;

import com.stockwise.models.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    Optional<Supplier> findByEmailAndDeletedFalse(String email);

    boolean existsByNameAndDeletedFalse(String name);

    List<Supplier> findAllByDeletedFalse();
}
