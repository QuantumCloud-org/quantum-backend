package com.alpha.system.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SysMenuMapperContractTest {
    @Test
    void roleMembershipQueriesIncludeOnlyActiveRoles() throws Exception {
        var config = readMapper();
        for (String query : new String[]{"selectPermsByUserId", "selectMenusByUserId"}) {
            var statement = config.getMappedStatement(SysMenuMapper.class.getName() + "." + query);
            assertThat(statement.getBoundSql(Map.of("userId", 2L)).getSql())
                    .contains("INNER JOIN sys_role r", "r.status = 1", "r.deleted = 0");
        }
    }

    @Test
    void recursionDeduplicatesAlreadyVisitedMenuRows() throws Exception {
        var statement = readMapper().getMappedStatement(SysMenuMapper.class.getName() + ".selectMenusByUserId");
        assertThat(statement.getBoundSql(Map.of("userId", 2L)).getSql())
                .contains("UNION")
                .doesNotContain("UNION ALL");
    }

    private Configuration readMapper() throws Exception {
        var config = new Configuration();
        String resource = "mapper/SysMenuMapper.xml";
        try (var xml = getClass().getClassLoader().getResourceAsStream(resource)) {
            new XMLMapperBuilder(xml, config, resource, config.getSqlFragments()).parse();
        }
        return config;
    }
}
