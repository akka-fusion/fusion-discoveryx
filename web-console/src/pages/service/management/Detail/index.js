/**
 * Date              Author           Des
 *----------------------------------------------
 * 2019/12/20        gongtiexin       服务详情
 * */

import React, { Component } from 'react';
import { inject, observer } from 'mobx-react';
import qs from 'query-string';
import {
  Descriptions,
  Divider,
  Form,
  Input,
  Modal,
  Popconfirm,
  Table,
  Radio,
  Row,
  Col,
  Icon,
} from 'antd';
import PropTypes from 'prop-types';

const CollectionCreateForm = Form.create({ name: 'form_in_modal' })(
  class extends React.Component {
    render() {
      const { visible, onCancel, onCreate, form, instance } = this.props;
      const { getFieldDecorator } = form;
      return (
        <Modal visible={visible} title="修改实例" onCancel={onCancel} onOk={onCreate} width={900}>
          <Form layout="vertical">
            <Row gutter={24}>
              <Col span={12}>
                <Form.Item label="IP">
                  {getFieldDecorator('ip', {
                    initialValue: instance?.ip,
                    rules: [{ required: true, message: '请输入IP!' }],
                  })(<Input />)}
                </Form.Item>
                <Form.Item label="端口">
                  {getFieldDecorator('port', {
                    initialValue: instance?.port,
                    rules: [{ required: true, message: '请输入端口!' }],
                  })(<Input />)}
                </Form.Item>
                <Form.Item label="权重">
                  {getFieldDecorator('weight', {
                    initialValue: instance?.weight,
                    rules: [{ required: true, message: '请输入权重!' }],
                  })(<Input />)}
                </Form.Item>
                <Form.Item label="元数据">
                  {getFieldDecorator('metadata', {
                    initialValue: JSON.stringify(instance?.metadata),
                    rules: [{ required: true, message: '请输入元数据!' }],
                  })(<Input />)}
                </Form.Item>
                <Form.Item label="健康状态">
                  {getFieldDecorator('healthy', {
                    initialValue: instance?.healthy,
                  })(
                    <Radio.Group>
                      <Radio value>健康</Radio>
                      <Radio value={false}>不健康</Radio>
                    </Radio.Group>,
                  )}
                </Form.Item>
                <Form.Item label="禁用/启用">
                  {getFieldDecorator('enabled', {
                    initialValue: instance?.enabled,
                  })(
                    <Radio.Group>
                      <Radio value>启用</Radio>
                      <Radio value={false}>禁用</Radio>
                    </Radio.Group>,
                  )}
                </Form.Item>
              </Col>
              <Col span={12}>
                <Form.Item label="healthyCheckMethod">
                  {getFieldDecorator('healthyCheckMethod', {
                    initialValue: instance?.healthyCheckMethod,
                    rules: [{ required: true, message: '请输入 healthyCheckMethod !' }],
                  })(<Input />)}
                </Form.Item>
                <Form.Item label="healthyCheckInterval">
                  {getFieldDecorator('healthyCheckInterval', {
                    initialValue: instance?.healthyCheckInterval,
                    rules: [{ required: true, message: '请输入 healthyCheckInterval !' }],
                  })(<Input />)}
                </Form.Item>
                <Form.Item label="unhealthyCheckCount">
                  {getFieldDecorator('unhealthyCheckCount', {
                    initialValue: instance?.unhealthyCheckCount,
                    rules: [{ required: true, message: '请输入 unhealthyCheckCount !' }],
                  })(<Input />)}
                </Form.Item>
                <Form.Item label="protocol">
                  {getFieldDecorator('protocol', {
                    initialValue: instance?.protocol,
                    rules: [{ required: true, message: '请输入 protocol !' }],
                  })(<Input />)}
                </Form.Item>
                <Form.Item label="useTls">
                  {getFieldDecorator('useTls', {
                    initialValue: instance?.useTls,
                    rules: [{ required: true, message: '请输入 useTls !' }],
                  })(<Input />)}
                </Form.Item>
                <Form.Item label="httpPath">
                  {getFieldDecorator('httpPath', {
                    initialValue: instance?.httpPath,
                  })(<Input />)}
                </Form.Item>
              </Col>
            </Row>
          </Form>
        </Modal>
      );
    }
  },
);

@inject(({ store: { serviceStore, namespaceStore } }) => ({ serviceStore, namespaceStore }))
@observer
export default class Detail extends Component {
  static propTypes = {
    serviceStore: PropTypes.object.isRequired,
  };

  formRef = React.createRef();

  state = {
    visible: false,
    loading: false,
    instance: null,
  };

  componentDidMount() {
    this.fetchData();
  }

  componentWillUnmount() {
    this.props.serviceStore.setService();
  }

  fetchData = () => {
    this.setState({ loading: true });
    this.props.serviceStore
      .getService(qs.parse(window.location.search))
      .finally(() => this.setState({ loading: false }));
  };

  showModal = instance => () => {
    this.setState({ visible: true, instance });
  };

  handleCancel = () => {
    this.setState({ visible: false, instance: null });
    this.formRef.current.props.form.resetFields();
  };

  handleUpdate = () => {
    const { form } = this.formRef.current.props;
    const { instance } = this.state;
    const {
      serviceStore: { service },
    } = this.props;

    form.validateFields((err, values) => {
      if (err) {
        return;
      }
      console.log('Received values of form: ', values);
      this.props.serviceStore
        .updateInstance({
          namespace: service.namespace,
          serviceName: service.serviceName,
          instanceId: instance.instanceId,
          ...values,
          metadata: JSON.parse(values.metadata),
        })
        .then(() => {
          this.fetchData();
          this.handleCancel();
        });
    });
  };

  handleDeleteInstance = instance => () => {
    this.props.serviceStore.deleteInstance(instance).then(this.fetchData);
  };

  render() {
    const {
      serviceStore: { service },
    } = this.props;

    const { loading, visible, instance } = this.state;

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
        render: text => <span>{text ? <Icon type="check" /> : <Icon type="close" />}</span>,
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
        render: text =>
          text ? (
            <Icon style={{ color: '#87d068' }} type="smile" />
          ) : (
            <Icon type="frown" style={{ color: '#f50' }} />
          ),
      },
      {
        title: '元数据',
        dataIndex: 'metadata',
        key: 'metadata',
        render: text => <span>{JSON.stringify(text)}</span>,
        width: '30%',
      },
      {
        title: '操作',
        dataIndex: 'action',
        key: 'action',
        width: '10%',
        render: (text, record) => (
          <span>
            <a onClick={this.showModal(record)} href="#">
              修改
            </a>
            <Divider type="vertical" />
            <Popconfirm
              title="确定要删除该实例吗?"
              onConfirm={this.handleDeleteInstance({
                namespace: service.namespace,
                serviceName: service.serviceName,
                instanceId: record.instanceId,
              })}
              okText="确认"
              cancelText="取消"
            >
              <a href="#">删除</a>
            </Popconfirm>
          </span>
        ),
      },
    ];

    return (
      <div>
        <Descriptions>
          <Descriptions.Item label="服务名称">{service.serviceName}</Descriptions.Item>
          <Descriptions.Item label="命名空间">{service.namespace}</Descriptions.Item>
          <Descriptions.Item label="分组">{service.groupName}</Descriptions.Item>
          <Descriptions.Item label="实例">
            <Table
              loading={loading}
              style={{ marginTop: 16 }}
              bordered
              dataSource={service.instances}
              columns={columns}
              pagination={false}
              rowKey="instanceId"
            />
          </Descriptions.Item>
        </Descriptions>
        <CollectionCreateForm
          instance={instance}
          wrappedComponentRef={this.formRef}
          visible={visible}
          onCancel={this.handleCancel}
          onCreate={this.handleUpdate}
        />
      </div>
    );
  }
}
