package com.fxz.exporter.elastic.objects;

import lombok.Data;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.Date;

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
