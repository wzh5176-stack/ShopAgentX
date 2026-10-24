package com.wwzyhy.shopagent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.wwzyhy.shopagent.dto.AddressResponse;
import com.wwzyhy.shopagent.dto.AddressWriteRequest;
import com.wwzyhy.shopagent.entity.UserAddress;
import com.wwzyhy.shopagent.exception.AddressNotFoundException;
import com.wwzyhy.shopagent.mapper.UserAddressMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddressService {

    private final CurrentUserService currentUserService;
    private final UserAddressMapper addressMapper;
    private final Validator validator;

    public AddressService(
            CurrentUserService currentUserService,
            UserAddressMapper addressMapper,
            Validator validator) {
        this.currentUserService = currentUserService;
        this.addressMapper = addressMapper;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> list(Authentication authentication) {
        Long userId = currentUserService.requireUserId(authentication);

        QueryWrapper<UserAddress> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        query.orderByDesc("is_default");
        query.orderByAsc("id");

        List<AddressResponse> items = new ArrayList<>();

        for (UserAddress address : addressMapper.selectList(query)) {
            items.add(AddressResponse.from(address));
        }

        return List.copyOf(items);
    }

    @Transactional(readOnly = true)
    public AddressResponse get(Authentication authentication, Long id) {
        validateId(id);
        Long userId = currentUserService.requireUserId(authentication);

        QueryWrapper<UserAddress> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        query.eq("id", id);

        UserAddress address = addressMapper.selectOne(query);

        if (address == null) {
            throw new AddressNotFoundException();
        }

        return AddressResponse.from(address);
    }

    @Transactional
    public AddressResponse create(
            Authentication authentication,
            AddressWriteRequest request) {
        validateRequest(request);
        Long userId = currentUserService.requireUserId(authentication);
        List<UserAddress> existing = lockAddresses(userId);

        UserAddress address = new UserAddress();
        address.setUserId(userId);
        address.setRecipientName(request.recipientName());
        address.setPhone(request.phone());
        address.setProvince(request.province());
        address.setCity(request.city());
        address.setDistrict(request.district());
        address.setDetailAddress(request.detailAddress());
        address.setDefaultFlag(existing.isEmpty() ? 1 : 0);

        if (addressMapper.insert(address) != 1
                || address.getId() == null) {
            throw new IllegalStateException("新增地址失败");
        }

        return readSaved(userId, address.getId());
    }

    @Transactional
    public AddressResponse update(
            Authentication authentication,
            Long id,
            AddressWriteRequest request) {
        validateId(id);
        validateRequest(request);

        Long userId = currentUserService.requireUserId(authentication);
        requireOwned(lockAddresses(userId), id);

        UpdateWrapper<UserAddress> update = new UpdateWrapper<>();
        update.eq("user_id", userId);
        update.eq("id", id);
        update.set("recipient_name", request.recipientName());
        update.set("phone", request.phone());
        update.set("province", request.province());
        update.set("city", request.city());
        update.set("district", request.district());
        update.set("detail_address", request.detailAddress());

        addressMapper.update(null, update);

        return readSaved(userId, id);
    }

    @Transactional
    public AddressResponse setDefault(
            Authentication authentication,
            Long id) {
        validateId(id);
        Long userId = currentUserService.requireUserId(authentication);

        UserAddress target = requireOwned(lockAddresses(userId), id);

        if (Integer.valueOf(1).equals(target.getDefaultFlag())) {
            return AddressResponse.from(target);
        }

        UpdateWrapper<UserAddress> clear = new UpdateWrapper<>();
        clear.eq("user_id", userId);
        clear.eq("is_default", 1);
        clear.set("is_default", 0);

        addressMapper.update(null, clear);

        markDefault(userId, id);

        return readSaved(userId, id);
    }

    @Transactional
    public void remove(Authentication authentication, Long id) {
        validateId(id);
        Long userId = currentUserService.requireUserId(authentication);

        List<UserAddress> addresses = lockAddresses(userId);
        UserAddress target = requireOwned(addresses, id);

        QueryWrapper<UserAddress> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        query.eq("id", id);

        if (addressMapper.delete(query) != 1) {
            throw new IllegalStateException("删除地址失败");
        }

        if (Integer.valueOf(1).equals(target.getDefaultFlag())) {
            for (UserAddress address : addresses) {
                if (!id.equals(address.getId())) {
                    markDefault(userId, address.getId());
                    break;
                }
            }
        }
    }

    private List<UserAddress> lockAddresses(Long userId) {
        QueryWrapper<UserAddress> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        query.orderByAsc("id");
        query.last("FOR UPDATE");

        return addressMapper.selectList(query);
    }

    private UserAddress requireOwned(
            List<UserAddress> addresses,
            Long id) {
        for (UserAddress address : addresses) {
            if (id.equals(address.getId())) {
                return address;
            }
        }

        throw new AddressNotFoundException();
    }

    private AddressResponse readSaved(Long userId, Long id) {
        QueryWrapper<UserAddress> query = new QueryWrapper<>();
        query.eq("user_id", userId);
        query.eq("id", id);
        query.last("FOR SHARE");

        UserAddress address = addressMapper.selectOne(query);

        if (address == null) {
            throw new IllegalStateException("读取保存后的地址失败");
        }

        return AddressResponse.from(address);
    }

    private void markDefault(Long userId, Long id) {
        UpdateWrapper<UserAddress> update = new UpdateWrapper<>();
        update.eq("user_id", userId);
        update.eq("id", id);
        update.set("is_default", 1);

        if (addressMapper.update(null, update) != 1) {
            throw new IllegalStateException("设置默认地址失败");
        }
    }

    private void validateRequest(AddressWriteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("地址信息不能为空");
        }

        Set<ConstraintViolation<AddressWriteRequest>> violations =
                validator.validate(request);

        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("地址信息不符合要求");
        }
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("地址编号必须为正整数");
        }
    }
}