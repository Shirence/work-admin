package com.example.workadmin.service;

import com.example.workadmin.common.BizException;
import com.example.workadmin.dto.MenuDtos;
import com.example.workadmin.entity.Menu;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 菜单与按钮权限，读写 stu 项目的 sys_menu 表。
 */
@Service
public class MenuService {

    private static final Set<String> TYPES = Set.of("DIRECTORY", "MENU", "BUTTON");

    private final StuStore store;

    public MenuService(StuStore store) {
        this.store = store;
    }

    public List<MenuDtos.MenuRow> list() {
        return store.listMenus().stream().map(MenuService::toRow).toList();
    }

    public List<MenuDtos.MenuRow> permissionTree() {
        return list();
    }

    @Transactional
    public MenuDtos.MenuRow create(MenuDtos.MenuRequest request) {
        Menu menu = new Menu();
        apply(menu, request);
        long id = store.insertMenu(menu.getParentId(), menu.getName(), menu.getType(), menu.getPath(),
                menu.getPermission(), menu.getSortOrder(), Boolean.TRUE.equals(menu.getEnabled()));
        return rowById(id);
    }

    @Transactional
    public MenuDtos.MenuRow update(Long id, MenuDtos.MenuRequest request) {
        StuStore.StuMenu existing = store.findMenuById(id)
                .orElseThrow(() -> new BizException(404, "菜单节点不存在"));
        if (existing.builtin()) {
            // 内置节点只允许改名称和排序，避免破坏前端的权限约定
            String name = request.name() == null || request.name().isBlank()
                    ? existing.name() : request.name().trim();
            int sort = request.sortOrder() == null ? existing.sortOrder()
                    : Math.max(0, Math.min(9999, request.sortOrder()));
            store.updateMenu(id, existing.parentId(), name, existing.type(), existing.path(),
                    existing.permission(), sort, existing.enabled());
            return rowById(id);
        }
        Menu menu = toMenu(existing);
        apply(menu, request);
        store.updateMenu(id, menu.getParentId(), menu.getName(), menu.getType(), menu.getPath(),
                menu.getPermission(), menu.getSortOrder(), Boolean.TRUE.equals(menu.getEnabled()));
        return rowById(id);
    }

    @Transactional
    public void delete(Long id) {
        StuStore.StuMenu menu = store.findMenuById(id)
                .orElseThrow(() -> new BizException(404, "菜单节点不存在"));
        if (menu.builtin()) {
            throw new BizException(409, "内置菜单不能删除");
        }
        if (store.listMenus().stream().anyMatch(m -> m.parentId() != null && m.parentId() == id)) {
            throw new BizException(409, "请先删除子节点");
        }
        store.deleteMenu(id);
    }

    private void apply(Menu menu, MenuDtos.MenuRequest request) {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) throw new BizException(400, "菜单名称不能为空");
        if (name.length() > 100) throw new BizException(400, "菜单名称不能超过 100 字");
        String type = normalizeType(request.type());
        Long parentId = request.parentId();

        Map<Long, StuStore.StuMenu> all = new HashMap<>();
        store.listMenus().forEach(m -> all.put(m.id(), m));

        if (parentId != null) {
            if (parentId.equals(menu.getId())) throw new BizException(400, "上级节点不能是自己");
            StuStore.StuMenu parent = all.get(parentId);
            if (parent == null) throw new BizException(400, "上级节点不存在");
            if (isDescendant(menu.getId(), parent, all)) {
                throw new BizException(400, "上级节点不能是自己的子节点");
            }
            if ("BUTTON".equals(type) && !"MENU".equals(parent.type())) {
                throw new BizException(400, "按钮的上级必须是页面");
            }
            if ("MENU".equals(type) && !"DIRECTORY".equals(parent.type())) {
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

    private boolean isDescendant(Long menuId, StuStore.StuMenu candidate, Map<Long, StuStore.StuMenu> all) {
        if (menuId == null) return false;
        StuStore.StuMenu current = candidate;
        int guard = 0;
        while (current != null && guard++ < 100) {
            if (current.id() == menuId) return true;
            current = current.parentId() == null ? null : all.get(current.parentId());
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

    private MenuDtos.MenuRow rowById(long id) {
        return store.findMenuById(id).map(MenuService::toRow)
                .orElseThrow(() -> new BizException(500, "读取菜单失败"));
    }

    private static Menu toMenu(StuStore.StuMenu menu) {
        Menu value = new Menu();
        value.setId(menu.id());
        value.setParentId(menu.parentId());
        value.setName(menu.name());
        value.setType(menu.type());
        value.setPath(menu.path());
        value.setPermission(menu.permission());
        value.setSortOrder(menu.sortOrder());
        value.setEnabled(menu.enabled());
        value.setBuiltin(menu.builtin());
        return value;
    }

    public static MenuDtos.MenuRow toRow(Menu menu) {
        return new MenuDtos.MenuRow(menu.getId(), menu.getParentId(), menu.getName(), menu.getType(),
                menu.getPath(), menu.getPermission(), menu.getSortOrder(), menu.getEnabled(), menu.getBuiltin());
    }

    public static MenuDtos.MenuRow toRow(StuStore.StuMenu menu) {
        return new MenuDtos.MenuRow(menu.id(), menu.parentId(), menu.name(), menu.type(),
                menu.path(), menu.permission(), menu.sortOrder(), menu.enabled(), menu.builtin());
    }
}
