/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       名称管理
 * */

import { action, observable } from 'mobx';
import request from '../utils/request';
import { PAGE_OBJECT } from '../utils/constants';

export default class NamespaceStore {
  /**
   * *************************** observable ***************************
   * */

  @observable
  namespacePage = PAGE_OBJECT;

  @observable
  namespaceList = [];

  /**
   * ****************************** ajax ******************************
   * */

  getNamespaceList = async () => {
    const {
      listed: { namespaces },
    } = await request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/management/ListNamespace',
        data: { page: 1, size: 100 },
      },
    });
    this.setNamespaceList(namespaces);
    return namespaces;
  };

  getNamespacePage = async data => {
    const { listed } = await request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/management/ListNamespace',
        data,
      },
    });
    const namespacePage = {
      page: listed.page,
      size: listed.size,
      totalElements: listed.totalElements,
      data: listed.namespaces,
    };
    this.setNamespacePage(namespacePage);
    return namespacePage;
  };

  createNamespace = data =>
    request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/management/CreateNamespace',
        data,
      },
      success: { message: '新建成功' },
      error: { message: '新建失败' },
    });

  deleteNamespace = data =>
    request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/management/RemoveNamespace',
        data,
      },
      success: { message: '删除成功' },
      error: { message: '删除失败' },
    });

  updateNamespace = data =>
    request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/management/ModifyNamespace',
        data,
      },
      success: { message: '编辑成功' },
      error: { message: '编辑失败' },
    });

  /**
   * ***************************** action *****************************
   * */

  @action
  setNamespacePage(data = PAGE_OBJECT) {
    this.namespacePage = data;
  }

  @action
  setNamespaceList(data = []) {
    this.namespaceList = data;
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
