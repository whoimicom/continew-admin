/*
 * Copyright (c) 2022-present Charles7c Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.charles7c.cnadmin.system.service.impl;

import java.util.List;
import java.util.Objects;

import javax.annotation.Resource;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import top.charles7c.cnadmin.common.base.BaseServiceImpl;
import top.charles7c.cnadmin.common.constant.SysConsts;
import top.charles7c.cnadmin.common.enums.DisEnableStatusEnum;
import top.charles7c.cnadmin.common.util.ExceptionUtils;
import top.charles7c.cnadmin.common.util.validate.CheckUtils;
import top.charles7c.cnadmin.system.mapper.DeptMapper;
import top.charles7c.cnadmin.system.model.entity.DeptDO;
import top.charles7c.cnadmin.system.model.query.DeptQuery;
import top.charles7c.cnadmin.system.model.request.DeptRequest;
import top.charles7c.cnadmin.system.model.vo.DeptDetailVO;
import top.charles7c.cnadmin.system.model.vo.DeptVO;
import top.charles7c.cnadmin.system.service.DeptService;
import top.charles7c.cnadmin.system.service.UserService;

/**
 * 部门业务实现类
 *
 * @author Charles7c
 * @since 2023/1/22 17:55
 */
@Service
@RequiredArgsConstructor
public class DeptServiceImpl extends BaseServiceImpl<DeptMapper, DeptDO, DeptVO, DeptDetailVO, DeptQuery, DeptRequest>
    implements DeptService {

    @Resource
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(DeptRequest request) {
        String name = request.getName();
        boolean isExists = this.checkNameExists(name, request.getParentId(), request.getId());
        CheckUtils.throwIf(() -> isExists, String.format("新增失败，'%s'已存在", name));

        request.setStatus(DisEnableStatusEnum.ENABLE);
        return super.add(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(DeptRequest request) {
        String name = request.getName();
        boolean isExists = this.checkNameExists(name, request.getParentId(), request.getId());
        CheckUtils.throwIf(() -> isExists, String.format("修改失败，'%s'已存在", name));

        super.update(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        CheckUtils.throwIf(() -> userService.countByDeptIds(ids) > 0, "所选部门存在用户关联，请解除关联后重试");
        super.delete(ids);
        baseMapper.lambdaUpdate().in(DeptDO::getParentId, ids).remove();
    }

    @Override
    public void fillDetail(Object detailObj) {
        super.fillDetail(detailObj);
        if (detailObj instanceof DeptDetailVO) {
            DeptDetailVO detailVO = (DeptDetailVO)detailObj;
            if (Objects.equals(SysConsts.SUPER_PARENT_ID, detailVO.getParentId())) {
                return;
            }
            detailVO.setParentName(ExceptionUtils.exToNull(() -> this.get(detailVO.getParentId()).getName()));
        }
    }

    /**
     * 检查名称是否存在
     *
     * @param name
     *            名称
     * @param parentId
     *            上级 ID
     * @param id
     *            ID
     * @return 是否存在
     */
    private boolean checkNameExists(String name, Long parentId, Long id) {
        return baseMapper.lambdaQuery().eq(DeptDO::getName, name).eq(DeptDO::getParentId, parentId)
            .ne(id != null, DeptDO::getId, id).exists();
    }
}
