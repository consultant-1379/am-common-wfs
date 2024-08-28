/*
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */
package com.ericsson.amcommonwfs.utils.repository;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

@Component
public class CamundaFileRepository implements RedisValuesRepository<String, byte[]> {

    private final ValueOperations<String, byte[]> valueOperations;
    private final RedisTemplate<String, byte[]> redisTemplate;

    @Autowired
    public CamundaFileRepository(@Qualifier("redisByteTemplate") RedisTemplate<String, byte[]> redisTemplate) {
        this.valueOperations = redisTemplate.opsForValue();
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String key, byte[] value, long timeoutInSec) {
        valueOperations.set(key, value);
        redisTemplate.expire(key, timeoutInSec, TimeUnit.SECONDS);
    }

    @Override
    public byte[] get(String key) {
        return valueOperations.get(key);
    }

}
