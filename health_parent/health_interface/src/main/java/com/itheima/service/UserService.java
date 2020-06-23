package com.itheima.service;

import com.itheima.pojo.Menu;
import com.itheima.pojo.User;

import java.util.List;

public interface UserService {
    public User findByUsername(String usernme);

    public List<Menu> getmenu(String username);
}
