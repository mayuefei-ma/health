package com.itheima.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.itheima.dao.PermissionDao;
import com.itheima.dao.RoleDao;
import com.itheima.dao.UserDao;
import com.itheima.pojo.Menu;
import com.itheima.pojo.Permission;
import com.itheima.pojo.Role;
import com.itheima.pojo.User;
import com.itheima.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 用户服务
 */
@Service(interfaceClass = UserService.class)
@Transactional
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;
    @Autowired
    private RoleDao roleDao;
    @Autowired
    private PermissionDao permissionDao;
    //根据用户名查询数据库获取用户信息和关联的角色信息，同时需要查询角色关联的权限信息
    @Override
    public User findByUsername(String usernme) {
        User user = userDao.findByUsername(usernme);//查询用户基本信息，不包含用户的角色
        if(user==null){
            return null;
        }
        Integer userId = user.getId();
        //根据用户ID查询对应的角色
        // SELECT *FROM  t_role WHERE id IN(SELECT role_id FROM t_user_role WHERE user_id='1')

        //SELECT	* FROM t_permission WHERE id IN (SELECT permission_id FROM t_role_permission WHERE role_id='1' )
        Set<Role> roles = roleDao.findByUserId(userId);
        user.setRoles(roles);//让用户关联角色
        for (Role role : roles) {
            Integer roleId = role.getId();
            //根据角色ID查询关联的权限
            Set<Permission> permissions = permissionDao.findByRoleId(roleId);
            role.setPermissions(permissions);//让角色关联权限
        }
        return user;
    }

    //根据用户命查询用户菜单信息
    @Override
    public List<Menu> getmenu(String username) {
        // 1) 查询用户所拥有的一级菜单,
       List<Menu> menuLeveOne= userDao.finMenuLeveOne(username);
        // 2) 循环一级菜单,
        for (Menu menu : menuLeveOne) {
            // 查询用户拥有的 当前一级菜单下的二级菜单
            List<Menu> menuLeveTwo=userDao.finMenuLeveTwo(username,menu.getId());
            menu.setChildren(menuLeveTwo);
        }
        return menuLeveOne;
    }
}
