package com.fxz.console.config;

import com.alibaba.fastjson.JSON;
import com.fxz.console.properties.PersistProperties;
import com.fxz.dnscore.objects.ARecord;
import com.fxz.fuled.config.starter.annotation.DimaondConfigChangeListener;
import com.fxz.fuled.config.starter.model.ConfigChangeEvent;
import com.fxz.queerer.CacheOperate;
import com.fxz.queerer.util.CacheUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Map;

@Slf4j
@Component
public class ConfigListener {

    @Autowired
    CacheOperate cacheOperate;

    @Autowired
    RedisTemplate redisTemplate;

    @Autowired
    PersistProperties persistProperties;

    @DimaondConfigChangeListener(interestedKeyPrefixes = "dns.persist")
    public void listen(ConfigChangeEvent configChangeEvent) {
        log.info("config changed values->{}", JSON.toJSONString(configChangeEvent.changedKeys()));
        if (!CollectionUtils.isEmpty(configChangeEvent.interestedChangedKeys())) {
            //del old
            configChangeEvent.interestedChangedKeys().stream().forEach(
                    c -> {
                        String oldValue = configChangeEvent.getChange(c).getOldValue();
                        if (StringUtils.hasText(oldValue)) {
                            for (String s : oldValue.split("\\,")) {
                                if (StringUtils.hasText(s)) {
                                    redisTemplate.delete(CacheUtil.assembleKey(s + ".", "A"));
                                }
                            }
                        }
                    }
            );
        }
        restore();
    }

    @PostConstruct
    public void init() {
        restore();
    }

    private void restore() {
        //add new
        if (!CollectionUtils.isEmpty(persistProperties.getConfig())) {
            for (Map.Entry<String, String> stringStringEntry : persistProperties.getConfig().entrySet()) {
                if (StringUtils.hasText(stringStringEntry.getValue())) {
                    for (String s : stringStringEntry.getValue().split("\\,")) {
                        if (StringUtils.hasText(s)) {
                            ARecord aRecord = new ARecord();
                            aRecord.setHost(s + ".");
                            aRecord.setType("A");
                            aRecord.setIpV4(stringStringEntry.getKey().replace("_", "."));
                            aRecord.setTtl(600);
                            redisTemplate.opsForValue().set("A_" + s + ".", JSON.toJSONString(Arrays.asList(aRecord)));
                        }
                    }
                }
            }
        }
        //aRecord.set
    }
}
