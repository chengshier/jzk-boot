package com.zbkj.service.service.impl.jiuzhoukang.dict;

import com.zbkj.service.service.jiuzhoukang.dict.JkDictService;
import com.zbkj.service.service.jiuzhoukang.support.JkDictLabelHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class JkDictRuntimeBridge {
    @Autowired private JkDictService dictService;

    @PostConstruct
    public void install() {
        JkDictLabelHelper.installResolver(new JkDictLabelHelper.Resolver() {
            @Override public String label(String dictType, String code) { return dictService.label(dictType, code); }
        });
    }
}
