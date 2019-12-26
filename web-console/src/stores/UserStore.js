/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       用户管理
 * */

import { action, observable } from 'mobx';
import request from '../utils/request';
import { PAGE_OBJECT } from '../utils/constants';

export default class UserStore {
  /**
   * *************************** observable ***************************
   * */

  @observable
  auth = {};

  @observable
  userPage = PAGE_OBJECT;

  /**
   * ****************************** ajax ******************************
   * */

  login = async data => {
    const { logined } = await request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/sign/Login',
        data,
      },
    });
    this.setAuth(logined);
    return logined;
  };

  logout = async data => {
    const logoutResponse = await request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/sign/Logout',
        data,
      },
    });
    this.setAuth();
    return logoutResponse;
  };

  getCurrentSessionUser = async data => {
    const { user } = await request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/sign/',
        data,
      },
    });
    this.setAuth(user);
    return user;
  };

  getUserPage = async data => {
    const { listed } = await request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/user/ListUser',
        data,
      },
    });
    const userPage = {
      page: listed.page,
      size: listed.size,
      totalElements: listed.totalElements,
      data: listed.users,
    };
    this.setUserPage(userPage);
    return userPage;
  };

  createUser = data =>
    request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/user/CreateUser',
        data,
      },
      success: { message: '新建成功' },
      error: { message: '新建失败' },
    });

  updateUser = data =>
    request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/user/ModifyUser',
        data,
      },
      success: { message: '编辑成功' },
      error: { message: '编辑失败' },
    });

  deleteUser = data =>
    request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/user/RemoveUser',
        data,
      },
      success: { message: '删除成功' },
      error: { message: '删除失败' },
    });

  /**
   * ***************************** action *****************************
   * */
  @action
  setAuth(data = {}) {
    this.auth = data;
  }

  @action
  setUserPage(data = PAGE_OBJECT) {
    this.userPage = data;
  }

  /**
   * **************************** computed ****************************
   * */
  // @computed
  // get computedData() {
  //   if (this.msg.length > 0) {
  //     return 'computed';
  //   }
  //   return [];
  // }
}
