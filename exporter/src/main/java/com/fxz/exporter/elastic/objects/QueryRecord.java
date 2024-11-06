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
    @Field(type = FieldType.Keyword)
    private String appName;
    @Field(type = FieldType.Keyword)
    private String id;
    @Field(type = FieldType.Ip)
    private String ip;
    @Field(type = FieldType.Keyword)
    private String host;
    @Field(type = FieldType.Keyword)
    private String queryType;
    private Integer answerCnt;
    @Field(type = FieldType.Keyword)
    private String dateStr;
    @Field(format = DateFormat.basic_date_time)
    private Date date;
    private String res;
    @Field(type = FieldType.Keyword)
    private String hostName;
    @Field(type = FieldType.Keyword)
    private String mac;
    @Field(type = FieldType.Keyword)
    private String country;
    @Field(type = FieldType.Keyword)
    private String province;
    @Field(type = FieldType.Keyword)
    private String city;
    @Field(type = FieldType.Keyword)
    private String isp;


}
