package com.itheima.dao;

import com.itheima.pojo.Menu;
import com.itheima.pojo.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserDao {
    public User findByUsername(String username);

    List<Menu> finMenuLeveOne(String username);

    List<Menu> finMenuLeveTwo(@Param("username") String username,@Param("pid") Integer id);
}
