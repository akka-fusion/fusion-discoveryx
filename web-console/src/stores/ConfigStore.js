/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       配置管理
 * */

import { action, observable } from 'mobx';
import request from '../utils/request';
import { PAGE_OBJECT } from '../utils/constants';

export default class ConfigStore {
  /**
   * *************************** observable ***************************
   * */

  @observable
  configPage = PAGE_OBJECT;

  @observable
  config = {};

  /**
   * ****************************** ajax ******************************
   * */

  getConfigPage = async data => {
    const { listed } = await request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/console/config/ListConfig',
        data,
      },
    });
    const configPage = {
      page: listed.page,
      size: listed.size,
      totalElements: listed.totalElements,
      data: listed.configs,
    };
    this.setConfigPage(configPage);
    return configPage;
  };

  getConfig = async data => {
    const { data: config } = await request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/v1/config/GetConfig',
        data,
      },
    });
    this.setConfig(config);
    return config;
  };

  createConfig = data =>
    request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/v1/config/PublishConfig',
        data,
      },
      success: { message: '新建成功' },
      error: { message: '新建失败' },
    });

  deleteConfig = data =>
    request({
      config: {
        method: 'POST',
        url: '/fusion/discoveryx/v1/config/RemoveConfig',
        data,
      },
      success: { message: '删除成功' },
      error: { message: '删除失败' },
    });

  /**
   * ***************************** action *****************************
   * */

  @action
  setConfigPage(data = PAGE_OBJECT) {
    this.configPage = data;
  }

  @action
  setConfig(data = {}) {
    this.config = data;
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
