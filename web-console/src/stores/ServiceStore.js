/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       服务管理
 * */

import { action, observable } from 'mobx';
import request from '../utils/request';
import { PAGE_OBJECT } from '../utils/constants';

export default class ServiceStore {
  /**
   * *************************** observable ***************************
   * */

  @observable
  servicePage = PAGE_OBJECT;

  @observable
  serviceList = [];

  @observable
  service = {};

  /**
   * ****************************** ajax ******************************
   * */

  getService = async data => {
    const { serviceInfo } = await request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/naming/GetService',
        data,
      },
    });
    this.setService(serviceInfo);
    return serviceInfo;
  };

  getServicePage = async data => {
    const { listedService } = await request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/naming/ListService',
        data,
      },
    });
    const servicePage = {
      page: listedService.page,
      size: listedService.size,
      totalElements: listedService.totalElements,
      data: listedService.serviceInfos,
    };
    this.setServicePage(servicePage);
    return servicePage;
  };

  createService = data =>
    request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/naming/CreateService',
        data,
      },
      success: { message: '新建成功' },
      error: { message: '新建失败' },
    });

  deleteService = data =>
    request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/naming/RemoveService',
        data,
      },
      success: { message: '删除成功' },
      error: { message: '删除失败' },
    });

  updateService = data =>
    request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/naming/ModifyService',
        data,
      },
      success: { message: '编辑成功' },
      error: { message: '编辑失败' },
    });

  /**
   * ***************************** action *****************************
   * */

  @action
  setServicePage(data = PAGE_OBJECT) {
    this.servicePage = data;
  }

  @action
  setServiceList(data = []) {
    this.serviceList = data;
  }

  @action
  setService(data = {}) {
    this.service = data;
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
