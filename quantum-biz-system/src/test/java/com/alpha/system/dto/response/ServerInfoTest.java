package com.alpha.system.dto.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ServerInfoTest {

    @Test
    void collectShouldPopulateJvmMaxAndDiskInfo() {
        ServerInfo serverInfo = new ServerInfo();

        serverInfo.collect();

        assertThat(serverInfo.getCpu()).isNotNull();
        assertThat(serverInfo.getMem()).isNotNull();
        assertThat(serverInfo.getJvm()).isNotNull();
        assertThat(serverInfo.getJvm().getMax()).isNotBlank();
        assertThat(serverInfo.getJvm().getUsed()).isNotBlank();
        assertThat(serverInfo.getSys()).isNotNull();
        assertThat(serverInfo.getSysFiles()).isNotNull();
        assertThat(serverInfo.getSysFiles()).isNotEmpty();
    }
}
