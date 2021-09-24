package com.fxz.console.controller;

import com.fxz.console.cache.Cache;
import com.fxz.console.cache.CacheOpTypeEnum;
import com.fxz.dnscore.annotation.Monitor;
import com.fxz.dnscore.objects.BaseRecord;
import com.fxz.exporter.elastic.baserepository.BaseSourceRepository;
import com.fxz.exporter.elastic.objects.SourceRecord;
import com.fxz.queerer.util.CacheUtil;
import com.fxz.queerer.util.ConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author fxz
 */
@RestController
@RequestMapping("/storage")
public class StorageController {

    @Autowired
    BaseSourceRepository baseSourceRepository;

    @PostMapping("/query")
    @Monitor
    @Cache(value = "fxz.dns.console.qry:",key = "{#host +':'+ #type}",expr = 1,unit = TimeUnit.HOURS)
    public List<BaseRecord> query(String host, String type) {
        if (StringUtils.hasText(host) && StringUtils.hasText(type)) {
            String key = CacheUtil.assembleKey(host, type.toUpperCase());
            Optional<SourceRecord> byId = baseSourceRepository.findById(key);
            if (byId.isPresent()) {
                List<BaseRecord> baseRecordList = ConvertUtil.decodeBaseRecordFromString(byId.get().getResult());
                return baseRecordList;
            }
        }
        return null;
    }

    @PostMapping("/del")
    @Monitor
    @Cache(value = "fxz.dns.console.qry:",key = "{#host +':'+ #type}",opType = CacheOpTypeEnum.DELETE)
    public Boolean del(String host, String type) {
        if (StringUtils.hasText(host) && StringUtils.hasText(type)) {
            String key = CacheUtil.assembleKey(host, type.toUpperCase());
            baseSourceRepository.deleteById(key);
            return true;
        }
        return false;
    }
}
