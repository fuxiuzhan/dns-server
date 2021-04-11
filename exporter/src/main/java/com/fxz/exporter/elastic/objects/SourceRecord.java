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
@Document(indexName = "dns_source")
public class SourceRecord implements Serializable {
    private String serverName;
    private String id;
    @Field(type = FieldType.Keyword)
    private String host;
    private String result;
    private String queryType;
    private Integer counter;
    private Long lastAccess;
    private Integer answerCnt;
}
