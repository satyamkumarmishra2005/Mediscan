package com.Mediscan.repository;

import com.Mediscan.model.MedicinePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MedicinePriceRepository extends JpaRepository<MedicinePrice, UUID> {
    List<MedicinePrice> findByMedicineId(UUID medicineId);

}
