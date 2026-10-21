package com.example.workadmin.repository;

import com.example.workadmin.entity.RoleMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface RoleMenuRepository extends JpaRepository<RoleMenu, Long> {

    List<RoleMenu> findByRoleId(Long roleId);

    void deleteByRoleId(Long roleId);

    @Transactional
    void deleteByMenuId(Long menuId);
}
