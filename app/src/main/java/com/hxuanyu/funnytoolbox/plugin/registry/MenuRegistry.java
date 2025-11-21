package com.hxuanyu.funnytoolbox.plugin.registry;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 菜单注册器
 */
@Component
public class MenuRegistry {

    private final List<MenuItem> menus = new CopyOnWriteArrayList<>();

    @Data
    public static class MenuItem {
        private String pluginId;
        private String label;
        private String icon;
        private String route;
        private int order;
    }

    /**
     * 注册菜单项
     */
    public void registerMenu(MenuItem item) {
        menus.add(item);
        menus.sort(Comparator.comparingInt(MenuItem::getOrder));
    }

    /**
     * 注销菜单项
     */
    public void unregisterMenu(String pluginId) {
        menus.removeIf(item -> item.getPluginId().equals(pluginId));
    }

    /**
     * 获取所有菜单
     */
    public List<MenuItem> getMenus() {
        return new ArrayList<>(menus);
    }
}
