package com.alpha.system.dto.response;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 路由配置
 */
@Data
public class RouterVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 路由名称
     */
    private String name;

    /**
     * 路由地址
     */
    private String path;

    /**
     * 是否隐藏
     */
    private Boolean hidden;

    /**
     * 重定向地址
     */
    private String redirect;

    /**
     * 组件路径
     */
    private String component;

    /**
     * 路由参数
     */
    private String query;

    /**
     * 是否总是显示
     */
    private Boolean alwaysShow;

    /**
     * 路由元信息
     */
    private MetaVO meta;

    /**
     * 子路由
     */
    private List<RouterVO> children;

    /**
     * 路由元信息
     */
    @Data
    public static class MetaVO implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * 标题
         */
        private String title;

        /**
         * 图标
         */
        private String icon;

        /**
         * 是否缓存
         */
        private Boolean noCache;

        /**
         * 是否需要权限
         */
        private Boolean requiresAuth;

        /**
         * 权限标识
         */
        private String permission;

        /**
         * 角色列表
         */
        private List<String> roles;

        /**
         * 链接地址
         */
        private String link;

        public MetaVO() {
        }

        public MetaVO(String title, String icon) {
            this.title = title;
            this.icon = icon;
        }

        public MetaVO(String title, String icon, Boolean noCache) {
            this.title = title;
            this.icon = icon;
            this.noCache = noCache;
        }

        public MetaVO(String title, String icon, String link) {
            this.title = title;
            this.icon = icon;
            this.link = link;
        }

        public MetaVO(String title, String icon, Boolean noCache, String link) {
            this.title = title;
            this.icon = icon;
            this.noCache = noCache;
            this.link = link;
        }

        public MetaVO(String title, String icon, Boolean noCache, String link, Boolean requiresAuth, String permission, List<String> roles) {
            this.title = title;
            this.icon = icon;
            this.noCache = noCache;
            this.link = link;
            this.requiresAuth = requiresAuth;
            this.permission = permission;
            this.roles = roles;
        }
    }
}
