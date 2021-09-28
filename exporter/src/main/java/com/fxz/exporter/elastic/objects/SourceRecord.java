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
    @Field(type = FieldType.Keyword)
    private String appName;
    @Field(type = FieldType.Keyword)
    private String id;
    @Field(type = FieldType.Keyword)
    private String host;
    private String result;
    @Field(type = FieldType.Keyword)
    private String queryType;
    @Field(type = FieldType.Keyword)
    private Integer cnt;
    @Field(format = DateFormat.basic_date_time)
    private Date lastAccess;
    @Field(type = FieldType.Keyword)
    private String dateStr;
    private Integer answerCnt;
}
