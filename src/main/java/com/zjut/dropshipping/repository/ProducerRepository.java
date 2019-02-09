package com.zjut.dropshipping.repository;


import com.zjut.dropshipping.dataobject.Producer;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author ljx
 */
public interface ProducerRepository extends JpaRepository<Producer, Integer> {

    Producer findOneById(Integer id);

    Integer countByName(String name);

    Integer countByContactPhone(String contactPhone);

    Integer countByContactIdentityNumber(String contactIdentityNumber);

    Integer countByLicenseNumber(String name);

    Producer findByContactPhoneAndPassword(String contactPhone, String password);
}