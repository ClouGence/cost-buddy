package com.costbuddy.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Reader;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class UserOwnedMapperXmlTest {

    private static final List<String> MAPPER_NAMES = List.of("CloudAccountMapper", "BillingAuditRunMapper", "BillingItemRuleMapper");

    @Test
    void ownedMapperStatementsContainMotherboardUserScope() throws Exception {
        for (String mapperName : MAPPER_NAMES) {
            Configuration configuration = parseMapper(mapperName);
            String namespace = "com.costbuddy.mapper." + mapperName;
            assertScoped(configuration, namespace + ".selectByIdAndMotherboardUserId");
            assertScoped(configuration, namespace + ".selectAllByMotherboardUserId");
            assertScoped(configuration, namespace + ".update");
            assertScoped(configuration, namespace + ".deleteByIdAndMotherboardUserId");
        }
    }

    @SuppressWarnings("deprecation")
    private Configuration parseMapper(String mapperName) throws Exception {
        String resource = "mapper/" + mapperName + ".xml";
        Configuration configuration = new Configuration();
        try (Reader reader = Resources.getResourceAsReader(resource)) {
            new XMLMapperBuilder(reader, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration;
    }

    private void assertScoped(Configuration configuration, String statementId) {
        MappedStatement statement = configuration.getMappedStatement(statementId);
        String sql = statement.getBoundSql(Map.of("id", 1L, "motherboardUserId", 42L)).getSql();
        assertThat(sql).as(statementId).contains("motherboard_user_id");
    }
}
