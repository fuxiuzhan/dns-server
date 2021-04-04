package com.fxz.queerer.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.fxz.dnscore.objects.*;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author fxz
 */
public class ConvertUtil {

    /**
     * 由于使用父类或者接口序列化转换，尤其是通过缓存中间件，会造成
     * 类型丢失，所以需要手动转换成对应的子类
     *
     * @param json
     * @return
     */
    public static List<BaseRecord> decodeBaseRecordFromString(String json) {
        if (StringUtils.hasText(json)) {
            JSONArray objects = JSON.parseArray(json);
            if (objects != null && objects.size() > 0) {
                List<BaseRecord> baseRecordList = new ArrayList<>();
                for (int i = 0; i < objects.size(); i++) {
                    String type = (String) JSON.parseObject(objects.get(i).toString()).get("type");
                    switch (type) {
                        case "AAAA":
                            baseRecordList.add(JSON.parseObject(objects.get(i).toString(), AAAARecord.class));
                            break;
                        case "A":
                            baseRecordList.add(JSON.parseObject(objects.get(i).toString(), ARecord.class));
                            break;
                        case "CNAME":
                            baseRecordList.add(JSON.parseObject(objects.get(i).toString(), CNAMERecord.class));
                            break;
                        case "MX":
                            baseRecordList.add(JSON.parseObject(objects.get(i).toString(), MXRecord.class));
                            break;
                        case "NS":
                            baseRecordList.add(JSON.parseObject(objects.get(i).toString(), NSRecord.class));
                            break;
                        case "PTR":
                            baseRecordList.add(JSON.parseObject(objects.get(i).toString(), PTRRecord.class));
                            break;
                        case "SOA":
                            baseRecordList.add(JSON.parseObject(objects.get(i).toString(), SOARecord.class));
                            break;
                        case "SRV":
                            baseRecordList.add(JSON.parseObject(objects.get(i).toString(), SRVRecord.class));
                            break;
                        case "TXT":
                            baseRecordList.add(JSON.parseObject(objects.get(i).toString(), TXTRecord.class));
                            break;
                        default:
                    }
                }
                return baseRecordList;
            }

        }
        return null;
    }

}
