package com.Mediscan.repository;

import com.Mediscan.model.MedicinePrice;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicinePriceRepository extends JpaRepository<MedicinePrice, UUID> {

    List<MedicinePrice> findByMedicineId(UUID medicineId);

    @Modifying
    @Transactional
    void deleteByMedicineId(UUID medicineId);
}
