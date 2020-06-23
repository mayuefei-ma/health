package com.itheima.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.itheima.constant.MessageConstant;
import com.itheima.entity.Result;
import com.itheima.pojo.Menu;
import com.itheima.service.UserService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户操作
 */
@RestController
@RequestMapping("/menu")
public class MenuController {
    @Reference
    private UserService userService;

    //获得当前登录用户的用户名
    @RequestMapping("/getmenu")
    public Result getmenu(){
        //当Spring security完成认证后，会将当前用户信息保存到框架提供的上下文对象
        User user =(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(user!=null){
            String username = user.getUsername();
            //根据用户命查询用户菜单信息
           List<Menu> list= userService.getmenu(username);
            return new Result(true, MessageConstant.GET_USERNAME_SUCCESS,list);
        }
        return new Result(false,MessageConstant.GET_USERNAME_FAIL);
    }
}
