package com.Mediscan.repository;

import com.Mediscan.model.GenericAlternative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface GenericAlternativeRepository extends JpaRepository<GenericAlternative, UUID> {
    List<GenericAlternative> findByMedicineId(UUID medicineId);  // ← lookup by medicine FK
    List<GenericAlternative> findBySaltCompositionContainingIgnoreCase(String saltComposition); // ← broader search
}
