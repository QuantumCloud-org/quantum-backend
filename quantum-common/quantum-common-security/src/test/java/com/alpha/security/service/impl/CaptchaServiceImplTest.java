package com.alpha.security.service.impl;

import com.alpha.cache.constant.CacheKeyConstant;
import com.alpha.cache.util.CacheClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaptchaServiceImplTest {
    @Test
    void consumesCaptchaUsingOneCacheOperation() {
        var cache = mock(CacheClient.class);
        String key = CacheKeyConstant.AUTH_CAPTCHA + "challenge";
        when(cache.<String>getAndDelete(key)).thenReturn("abcd", (String) null);
        var service = new CaptchaServiceImpl(cache);
        assertThat(service.verify("challenge", "ABCD")).isTrue();
        assertThat(service.verify("challenge", "ABCD")).isFalse();
        verify(cache, org.mockito.Mockito.times(2)).getAndDelete(key);
    }
}
