/**
 * Date              Author           Des
 *----------------------------------------------
 * 2019/12/20        gongtiexin       服务详情
 * */

import React, { Component } from 'react';
import { inject, observer } from 'mobx-react';
import qs from 'query-string';
import { Descriptions, Divider, Popconfirm, Table } from 'antd';
import PropTypes from 'prop-types';
import { Link } from 'react-router-dom';
import { SERVICE_MANAGEMENT_DETAIL } from '../../../../router/constants';

@inject(({ store: { serviceStore, namespaceStore } }) => ({ serviceStore, namespaceStore }))
@observer
export default class Detail extends Component {
  static propTypes = {
    serviceStore: PropTypes.object.isRequired,
  };

  componentDidMount() {
    this.props.serviceStore.getService(qs.parse(window.location.search));
  }

  componentWillUnmount() {
    this.props.serviceStore.setService();
  }

  render() {
    const {
      serviceStore: { service },
    } = this.props;

    const columns = [
      {
        title: 'IP',
        dataIndex: 'ip',
        key: 'ip',
      },
      {
        title: '端口',
        dataIndex: 'port',
        key: 'port',
      },
      {
        title: '临时实例',
        dataIndex: 'ephemeral',
        key: 'ephemeral',
        render: text => <span>{text.toString()}</span>,
      },
      {
        title: '权重',
        dataIndex: 'weight',
        key: 'weight',
      },
      {
        title: '健康状态',
        dataIndex: 'healthy',
        key: 'healthy',
        render: text => <span>{text.toString()}</span>,
      },
      {
        title: '元数据',
        dataIndex: 'port',
        key: 'port',
      },
      {
        title: '操作',
        dataIndex: 'action',
        key: 'action',
        width: '10%',
        render: (text, record) => (
          <span>
            {/* <Link */}
            {/*  to={`${SERVICE_MANAGEMENT_DETAIL}?${qs.stringify({ */}
            {/*    namespace: record.namespace, */}
            {/*    groupName: record.groupName, */}
            {/*    serviceName: record.serviceName, */}
            {/*  })}`} */}
            {/* > */}
            {/*  详情 */}
            {/* </Link> */}
            {/* <Divider type="vertical" /> */}
            {/* <Popconfirm */}
            {/*  title="确定要删除该服务吗?" */}
            {/*  onConfirm={this.handleDeleteService(record.serviceName)} */}
            {/*  okText="确认" */}
            {/*  cancelText="取消" */}
            {/* > */}
            {/*  <a href="#">删除</a> */}
            {/* </Popconfirm> */}
          </span>
        ),
      },
    ];

    return (
      <Descriptions>
        <Descriptions.Item label="服务名称">{service.serviceName}</Descriptions.Item>
        <Descriptions.Item label="命名空间">{service.namespace}</Descriptions.Item>
        <Descriptions.Item label="分组">{service.groupName}</Descriptions.Item>
        <Descriptions.Item label="实例">
          <Table
            style={{ marginTop: 16 }}
            bordered
            dataSource={service.instances}
            columns={columns}
            pagination={false}
          />
        </Descriptions.Item>
      </Descriptions>
    );
  }
}
