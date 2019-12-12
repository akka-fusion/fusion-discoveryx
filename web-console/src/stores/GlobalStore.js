/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       global
 * */

import { action, observable } from 'mobx';
import request from '../utils/request';

export default class GlobalStore {
  /**
   * *************************** observable ***************************
   * */

  @observable
  list = [];

  /**
   * ****************************** ajax ******************************
   * */

  /**
   * 获取数据
   * */
  getList = async params => {
    const { data } = await request({
      config: { method: 'GET', url: '/api/list', params },
    });
    this.setList(data);
    return data;
  };

  /**
   * ***************************** action *****************************
   * */

  @action
  setList(data = []) {
    this.list = data;
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
