package com.fxz.exporter.elastic.objects;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;

/**
 * @author fxz
 */
@Data
@Document(indexName = "dns_source")
public class SourceRecord implements Serializable {
    private String serverName;
    private String id;
    private String result;
    private String queryType;
    private Integer counter;
    private Long lastAccess;
    private Integer answerCnt;
}
