package com.cc.job.system.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.job.system.model.bo.UserBO;
import com.cc.job.system.model.dto.UserImportDTO;
import com.cc.job.system.model.entity.User;
import com.cc.job.system.model.form.UserForm;
import com.cc.job.system.model.form.UserProfileForm;
import com.cc.job.system.model.vo.UserInfoVO;
import com.cc.job.system.model.vo.UserPageVO;
import com.cc.job.system.model.vo.UserProfileVO;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * 用户对象转换器
 *
 * @author haoxr
 * @since 2022/6/8
 */
@Mapper(componentModel = "spring")
public interface UserConverter {

    UserPageVO toPageVo(UserBO bo);

    Page<UserPageVO> toPageVo(Page<UserBO> bo);

    UserForm toForm(User entity);

    @InheritInverseConfiguration(name = "toForm")
    User toEntity(UserForm entity);

    @Mappings({
            @Mapping(target = "userId", source = "id")
    })
    UserInfoVO toUserInfoVo(User entity);

    User toEntity(UserImportDTO vo);


    UserProfileVO toProfileVO(UserBO bo);

    User toEntity(UserProfileForm formData);
}
