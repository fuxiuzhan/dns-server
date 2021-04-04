package com.fxz.exporter.mysql.objects;

import lombok.Data;

import java.io.Serializable;

/**
 * @author fxz
 */
@Data
public class QueryRecord implements Serializable {
    private Long id;
    private String ip;
    private String host;
    private String queryType;
    private Integer answerCnt;
    private String date;
    private String res;
}
