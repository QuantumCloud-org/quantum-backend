package com.alpha.system.mapper;

import com.alpha.system.domain.SysMenu;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 菜单 Mapper
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 根据用户ID查询权限标识
     */
    Set<String> selectPermsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID查询菜单列表（包含所有祖先菜单）
     */
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId);

}
