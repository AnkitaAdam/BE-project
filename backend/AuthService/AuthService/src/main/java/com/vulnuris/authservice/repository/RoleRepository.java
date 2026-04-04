package com.vulnuris.authservice.repository;

import com.vulnuris.authservice.entity.Role;
import com.vulnuris.authservice.entity.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
