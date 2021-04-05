package com.fxz.exporter.elastic.baserepository;

import com.fxz.exporter.elastic.objects.QueryRecord;
import com.fxz.exporter.elastic.objects.SourceRecord;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @author fxz
 */
public interface BaseRecordRepository extends ElasticsearchRepository<QueryRecord, String> {
}
