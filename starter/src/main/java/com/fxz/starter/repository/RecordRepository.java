package com.fxz.starter.repository;


import com.fxz.exporter.elastic.objects.QueryRecord;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

/**
 * @author fxz
 */
@Component
public interface RecordRepository extends ElasticsearchRepository<QueryRecord, String> {

}
