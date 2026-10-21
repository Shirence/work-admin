package com.example.workadmin.repository;

import com.example.workadmin.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findAllByOrderBySortOrderAscIdAsc();

    boolean existsByParentId(Long parentId);
}
