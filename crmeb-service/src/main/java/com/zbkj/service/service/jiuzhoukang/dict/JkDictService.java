package com.zbkj.service.service.jiuzhoukang.dict;

import com.zbkj.common.model.jiuzhoukang.JkDictItem;
import com.zbkj.common.model.jiuzhoukang.JkDictType;
import com.zbkj.common.request.jiuzhoukang.JkDictItemSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkDictTypeSaveRequest;

import java.util.List;

public interface JkDictService {
    List<JkDictType> listTypes(String keywords);
    List<JkDictItem> listItems(String dictType, Boolean enabledOnly);
    JkDictType saveType(JkDictTypeSaveRequest request);
    JkDictItem saveItem(JkDictItemSaveRequest request);
    boolean updateTypeStatus(Long id, boolean status);
    boolean updateItemStatus(Long id, boolean status);
    String label(String dictType, String code);
    String tag(String dictType, String code);
    void clearCache();
}
