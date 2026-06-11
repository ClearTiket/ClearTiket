package com.clearticket.clearticket.repository;

import com.clearticket.clearticket.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

}
