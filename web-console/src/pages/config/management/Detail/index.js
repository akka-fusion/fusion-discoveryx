/**
 * Date              Author           Des
 *----------------------------------------------
 * 2019/12/20        gongtiexin       服务详情
 * */

import React, { Component } from 'react';
import { inject, observer } from 'mobx-react';
import qs from 'query-string';
import { Descriptions, Table } from 'antd';
import PropTypes from 'prop-types';
import { CONFIG_TYPE_ENUM } from '../../constants';

@inject(({ store: { configStore } }) => ({ configStore }))
@observer
export default class Detail extends Component {
  static propTypes = {
    configStore: PropTypes.object.isRequired,
  };

  componentDidMount() {
    this.props.configStore.getConfig(qs.parse(window.location.search));
  }

  componentWillUnmount() {
    this.props.configStore.setConfig();
  }

  render() {
    const {
      configStore: { config },
    } = this.props;

    return (
      <Descriptions column={1}>
        <Descriptions.Item label="Namespace">{config.namespace}</Descriptions.Item>
        <Descriptions.Item label="Data Id">{config.dataId}</Descriptions.Item>
        <Descriptions.Item label="Group">{config.groupName}</Descriptions.Item>
        <Descriptions.Item label="Type">
          {CONFIG_TYPE_ENUM.properties[config.type]?.label}
        </Descriptions.Item>
        <Descriptions.Item label="content">{config.content}</Descriptions.Item>
      </Descriptions>
    );
  }
}
