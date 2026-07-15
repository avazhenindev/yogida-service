package com.yogida.meditation.service;

import com.yogida.meditation.dto.AppUserDto;
import com.yogida.meditation.service.api.AppUserApi;
import com.yogida.meditation.service.api.UserFacadeApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserFacadeService implements UserFacadeApi {

    private final AppUserApi appUserApi;

    @Override
    @Transactional
    public AppUserDto onboardUser(AppUserDto dto) {
        AppUserDto createdUser = appUserApi.create(dto);
        log.info("UserFacadeService > User created with id: {}", createdUser.getUserId());
        return createdUser;
    }
}
