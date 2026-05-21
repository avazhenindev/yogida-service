package com.yogida.meditation.service.api;

import com.yogida.meditation.dto.AppUserDto;

public interface UserFacadeApi {

    /**
     * Onboards a new user: creates the user account and assigns the default FREE subscription
     * within a single transaction.
     *
     * @param dto user data
     * @return created user DTO with the assigned subscription
     */
    AppUserDto onboardUser(AppUserDto dto);
}

