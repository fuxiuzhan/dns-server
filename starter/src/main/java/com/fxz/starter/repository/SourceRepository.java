package com.fxz.starter.repository;


import com.fxz.exporter.elastic.objects.SourceRecord;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

/**
 * @author fxz
 */
@Component
public interface SourceRepository extends ElasticsearchRepository<SourceRecord, String> {

}
