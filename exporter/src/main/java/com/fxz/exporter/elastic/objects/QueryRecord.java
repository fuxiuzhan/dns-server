package com.fxz.exporter.elastic.objects;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;

/**
 * @author fxz
 */
@Data
@Document(indexName = "dns_query_record")
public class QueryRecord implements Serializable {
    private String serverName;
    private String id;
    private String ip;
    private String host;
    private String queryType;
    private Integer answerCnt;
    private String date;
    private String res;
    private long timeMillis;
}
