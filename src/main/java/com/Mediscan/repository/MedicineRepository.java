package com.Mediscan.repository;

import com.Mediscan.model.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, UUID> {
    List<Medicine> findByBrandNameContainingIgnoreCase(String brandName);
    List<Medicine> findBySaltCompositionContainingIgnoreCase(String saltComposition);
}
