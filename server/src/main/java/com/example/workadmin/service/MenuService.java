package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.dto.MenuDtos;
import com.example.workadmin.entity.Menu;
import com.example.workadmin.repository.MenuRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class MenuService {

    private static final Set<String> TYPES = Set.of("DIRECTORY", "MENU", "BUTTON");

    private final MenuRepository menuRepository;

    public MenuService(MenuRepository menuRepository) {
        this.menuRepository = menuRepository;
    }

    public List<MenuDtos.MenuRow> list() {
        return menuRepository.findAllByOrderBySortOrderAscIdAsc().stream().map(MenuService::toRow).toList();
    }

    public List<MenuDtos.MenuRow> permissionTree() {
        return list();
    }

    @Transactional
    public MenuDtos.MenuRow create(MenuDtos.MenuRequest request) {
        Menu menu = new Menu();
        apply(menu, request);
        menu.setBuiltin(false);
        return toRow(menuRepository.save(menu));
    }

    @Transactional
    public MenuDtos.MenuRow update(Long id, MenuDtos.MenuRequest request) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "菜单节点不存在"));
        if (Boolean.TRUE.equals(menu.getBuiltin())) {
            // 内置节点只允许改名称和排序，避免破坏前端约定。
            if (request.name() != null && !request.name().isBlank()) {
                menu.setName(request.name().trim());
            }
            if (request.sortOrder() != null) {
                menu.setSortOrder(Math.max(0, Math.min(9999, request.sortOrder())));
            }
            return toRow(menuRepository.save(menu));
        }
        apply(menu, request);
        return toRow(menuRepository.save(menu));
    }

    @Transactional
    public void delete(Long id) {
        Menu menu = menuRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "菜单节点不存在"));
        if (Boolean.TRUE.equals(menu.getBuiltin())) {
            throw new BizException(409, "内置菜单不能删除");
        }
        if (menuRepository.existsByParentId(id)) {
            throw new BizException(409, "请先删除子节点");
        }
        menuRepository.delete(menu);
    }

    private void apply(Menu menu, MenuDtos.MenuRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) throw new BizException(400, "菜单名称不能为空");
        if (name.length() > 100) throw new BizException(400, "菜单名称不能超过 100 字");
        String type = normalizeType(request.type());
        Long parentId = request.parentId();
        if (parentId != null) {
            if (parentId.equals(menu.getId())) throw new BizException(400, "上级节点不能是自己");
            Menu parent = menuRepository.findById(parentId)
                    .orElseThrow(() -> new BizException(400, "上级节点不存在"));
            if (isDescendant(menu.getId(), parent)) {
                throw new BizException(400, "上级节点不能是自己的子节点");
            }
            if ("BUTTON".equals(type) && !"MENU".equals(parent.getType())) {
                throw new BizException(400, "按钮的上级必须是页面");
            }
            if ("MENU".equals(type) && !"DIRECTORY".equals(parent.getType())) {
                throw new BizException(400, "页面的上级必须是目录");
            }
        }
        String path = trim(request.path());
        String permission = trim(request.permission());
        if ("MENU".equals(type) && path == null) throw new BizException(400, "页面必须填写路径");
        if ("DIRECTORY".equals(type)) {
            path = null;
            permission = null;
        }
        if ("BUTTON".equals(type)) {
            path = null;
            if (permission == null) throw new BizException(400, "按钮必须填写权限标识");
        }
        if (path != null && path.length() > 200) throw new BizException(400, "路径不能超过 200 字");
        if (permission != null && permission.length() > 100) throw new BizException(400, "权限标识不能超过 100 字");
        menu.setParentId(parentId);
        menu.setName(name);
        menu.setType(type);
        menu.setPath(path);
        menu.setPermission(permission);
        menu.setSortOrder(request.sortOrder() == null ? 0 : Math.max(0, Math.min(9999, request.sortOrder())));
        menu.setEnabled(request.enabled() == null || request.enabled());
    }

    private boolean isDescendant(Long menuId, Menu candidate) {
        if (menuId == null) return false;
        long guard = 0;
        Menu current = candidate;
        while (current != null && guard++ < 100) {
            if (menuId.equals(current.getId())) return true;
            current = current.getParentId() == null ? null
                    : menuRepository.findById(current.getParentId()).orElse(null);
        }
        return false;
    }

    private String normalizeType(String raw) {
        String type = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (!TYPES.contains(type)) throw new BizException(400, "节点类型不合法");
        return type;
    }

    private String trim(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static MenuDtos.MenuRow toRow(Menu menu) {
        return new MenuDtos.MenuRow(menu.getId(), menu.getParentId(), menu.getName(), menu.getType(),
                menu.getPath(), menu.getPermission(), menu.getSortOrder(), menu.getEnabled(), menu.getBuiltin());
    }
}
