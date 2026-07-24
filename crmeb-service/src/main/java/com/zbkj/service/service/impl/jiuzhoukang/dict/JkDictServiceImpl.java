package com.zbkj.service.service.impl.jiuzhoukang.dict;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zbkj.common.model.jiuzhoukang.JkDictItem;
import com.zbkj.common.model.jiuzhoukang.JkDictType;
import com.zbkj.common.request.jiuzhoukang.JkDictItemSaveRequest;
import com.zbkj.common.request.jiuzhoukang.JkDictTypeSaveRequest;
import com.zbkj.service.dao.jiuzhoukang.JkDictItemDao;
import com.zbkj.service.dao.jiuzhoukang.JkDictTypeDao;
import com.zbkj.service.service.jiuzhoukang.dict.JkDictService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JkDictServiceImpl implements JkDictService {
    @Autowired private JkDictTypeDao typeDao;
    @Autowired private JkDictItemDao itemDao;

    private final Map<String, Map<String, JkDictItem>> cache = new ConcurrentHashMap<>();

    @Override
    public List<JkDictType> listTypes(String keywords) {
        LambdaQueryWrapper<JkDictType> query = new LambdaQueryWrapper<JkDictType>()
                .eq(JkDictType::getIsDeleted, false).orderByAsc(JkDictType::getDictType);
        if (StrUtil.isNotBlank(keywords)) {
            query.and(w -> w.like(JkDictType::getDictType, keywords.trim()).or().like(JkDictType::getDictName, keywords.trim()));
        }
        return typeDao.selectList(query);
    }

    @Override
    public List<JkDictItem> listItems(String dictType, Boolean enabledOnly) {
        if (StrUtil.isBlank(dictType)) return Collections.emptyList();
        LambdaQueryWrapper<JkDictItem> query = new LambdaQueryWrapper<JkDictItem>()
                .eq(JkDictItem::getDictType, dictType.trim()).eq(JkDictItem::getIsDeleted, false)
                .orderByAsc(JkDictItem::getSort).orderByAsc(JkDictItem::getId);
        if (Boolean.TRUE.equals(enabledOnly)) query.eq(JkDictItem::getStatus, true);
        return itemDao.selectList(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkDictType saveType(JkDictTypeSaveRequest request) {
        Date now = new Date();
        JkDictType entity = request.getId() == null ? new JkDictType() : typeDao.selectById(request.getId());
        if (entity == null) throw new IllegalArgumentException("字典类型不存在");
        JkDictType duplicate = typeDao.selectOne(new LambdaQueryWrapper<JkDictType>()
                .eq(JkDictType::getDictType, request.getDictType().trim()).eq(JkDictType::getIsDeleted, false)
                .ne(request.getId() != null, JkDictType::getId, request.getId()).last("limit 1"));
        if (duplicate != null) throw new IllegalArgumentException("字典类型编码已存在");
        BeanUtils.copyProperties(request, entity);
        entity.setDictType(request.getDictType().trim()).setDictName(request.getDictName().trim())
                .setStatus(request.getStatus() == null || request.getStatus()).setIsDeleted(false).setUpdateTime(now);
        if (entity.getId() == null) { entity.setCreateTime(now); typeDao.insert(entity); }
        else typeDao.updateById(entity);
        clearCache();
        return entity;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JkDictItem saveItem(JkDictItemSaveRequest request) {
        JkDictType type = typeDao.selectOne(new LambdaQueryWrapper<JkDictType>()
                .eq(JkDictType::getDictType, request.getDictType().trim()).eq(JkDictType::getIsDeleted, false).last("limit 1"));
        if (type == null) throw new IllegalArgumentException("字典类型不存在");
        Date now = new Date();
        JkDictItem entity = request.getId() == null ? new JkDictItem() : itemDao.selectById(request.getId());
        if (entity == null) throw new IllegalArgumentException("字典项不存在");
        JkDictItem duplicate = itemDao.selectOne(new LambdaQueryWrapper<JkDictItem>()
                .eq(JkDictItem::getDictType, request.getDictType().trim())
                .eq(JkDictItem::getItemCode, request.getItemCode().trim())
                .eq(JkDictItem::getIsDeleted, false)
                .ne(request.getId() != null, JkDictItem::getId, request.getId()).last("limit 1"));
        if (duplicate != null) throw new IllegalArgumentException("同一字典类型下编码已存在");
        BeanUtils.copyProperties(request, entity);
        entity.setDictType(request.getDictType().trim()).setItemCode(request.getItemCode().trim())
                .setItemLabel(request.getItemLabel().trim()).setSort(request.getSort() == null ? 0 : request.getSort())
                .setStatus(request.getStatus() == null || request.getStatus()).setIsDeleted(false).setUpdateTime(now);
        if (entity.getId() == null) { entity.setCreateTime(now); itemDao.insert(entity); }
        else itemDao.updateById(entity);
        clearCache();
        return entity;
    }

    @Override public boolean updateTypeStatus(Long id, boolean status) {
        JkDictType entity = typeDao.selectById(id); if (entity == null) return false;
        entity.setStatus(status).setUpdateTime(new Date()); boolean result = typeDao.updateById(entity) == 1; clearCache(); return result;
    }
    @Override public boolean updateItemStatus(Long id, boolean status) {
        JkDictItem entity = itemDao.selectById(id); if (entity == null) return false;
        entity.setStatus(status).setUpdateTime(new Date()); boolean result = itemDao.updateById(entity) == 1; clearCache(); return result;
    }

    @Override public String label(String dictType, String code) {
        JkDictItem item = find(dictType, code); return item == null ? null : item.getItemLabel();
    }
    @Override public String tag(String dictType, String code) {
        JkDictItem item = find(dictType, code); return item == null ? null : item.getItemTag();
    }
    @Override public void clearCache() { cache.clear(); }

    private JkDictItem find(String dictType, String code) {
        if (StrUtil.isBlank(dictType) || StrUtil.isBlank(code)) return null;
        Map<String, JkDictItem> values = cache.computeIfAbsent(dictType, key -> {
            Map<String, JkDictItem> map = new ConcurrentHashMap<>();
            for (JkDictItem item : listItems(key, true)) map.put(item.getItemCode(), item);
            return map;
        });
        return values.get(code);
    }
}
