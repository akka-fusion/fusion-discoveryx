import React, { Component } from 'react';
import { inject, observer } from 'mobx-react';
import { Link } from 'react-router-dom';
import qs from 'query-string';
import { Button, Divider, Form, Input, Modal, Popconfirm } from 'antd';
import PropTypes from 'prop-types';
import { upStorage } from 'up-utils';
import SearchTable from '../../../../components/SearchTable';
import NamespaceChoose from '../../../../components/NamespaceChoose';
import { SERVICE_MANAGEMENT_DETAIL } from '../../../../router/constants';

const fields = [
  {
    type: 'input',
    key: 'serviceName',
    label: '服务名称',
  },
];

const CollectionCreateForm = Form.create({ name: 'form_in_modal' })(
  class extends React.Component {
    render() {
      const { visible, onCancel, onCreate, form } = this.props;
      const { getFieldDecorator } = form;
      return (
        <Modal visible={visible} title="新建服务" onCancel={onCancel} onOk={onCreate}>
          <Form layout="vertical">
            <Form.Item label="服务名称">
              {getFieldDecorator('serviceName', {
                rules: [{ required: true, message: '请输入服务名称!' }],
              })(<Input />)}
            </Form.Item>
            <Form.Item label="服务名称">{getFieldDecorator('groupName', {})(<Input />)}</Form.Item>
          </Form>
        </Modal>
      );
    }
  },
);

@inject(({ store: { serviceStore } }) => ({ serviceStore }))
@observer
export default class List extends Component {
  searchTableRef = React.createRef();

  formRef = React.createRef();

  state = { visible: false };

  static propTypes = {
    serviceStore: PropTypes.object.isRequired,
  };

  componentDidMount() {}

  componentWillUnmount() {
    this.props.serviceStore.setServicePage();
  }

  handleDeleteService = serviceName => () =>
    this.props.serviceStore
      .deleteService({ serviceName, namespace: upStorage.getSession('namespace') })
      .then(this.searchTableRef.current.fetchData);

  fetchData = namespace => {
    this.props.serviceStore.setServicePage();
    this.searchTableRef.current.handleReset();
    this.searchTableRef.current.fetchData({ namespace, page: 1 });
  };

  getServicePage = params =>
    this.props.serviceStore.getServicePage({
      namespace: upStorage.getSession('namespace'),
      ...params,
    });

  showModal = () => {
    this.setState({ visible: true });
  };

  handleCancel = () => {
    this.setState({ visible: false });
    this.formRef.current.props.form.resetFields();
  };

  handleCreate = () => {
    const { form } = this.formRef.current.props;

    form.validateFields((err, values) => {
      if (err) {
        return;
      }
      console.log('Received values of form: ', values);
      this.props.serviceStore
        .createService({
          namespace: upStorage.getSession('namespace'),
          ...values,
        })
        .then(() => {
          this.searchTableRef.current.fetchData();
          this.handleCancel();
        });
    });
  };

  render() {
    const {
      serviceStore: {
        servicePage: { page, size, totalElements, data },
      },
    } = this.props;
    const { visible } = this.state;

    const expandChildren = (
      <Button onClick={this.showModal} type="primary">
        新建服务
      </Button>
    );

    const columns = [
      {
        title: '服务名称',
        dataIndex: 'serviceName',
        key: 'serviceName',
      },
      {
        title: '分组',
        dataIndex: 'groupName',
        key: 'groupName',
      },
      {
        title: '实例数',
        dataIndex: 'instances',
        key: 'instances',
        render: text => text.length,
      },
      {
        title: '操作',
        dataIndex: 'action',
        key: 'action',
        width: '10%',
        render: (text, record) => (
          <span>
            <Link
              to={`${SERVICE_MANAGEMENT_DETAIL}?${qs.stringify({
                namespace: record.namespace,
                groupName: record.groupName,
                serviceName: record.serviceName,
              })}`}
            >
              详情
            </Link>
            <Divider type="vertical" />
            <Popconfirm
              title="确定要删除该服务吗?"
              onConfirm={this.handleDeleteService(record.serviceName)}
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
        <NamespaceChoose fetchData={this.fetchData} />
        <SearchTable
          firstFetch={false}
          ref={this.searchTableRef}
          expandChildren={expandChildren}
          callback={this.getServicePage}
          fields={fields}
          tableProps={{ dataSource: data, columns, rowKey: 'namespace' }}
          paginationProps={{
            page,
            size,
            totalElements,
          }}
        />
        <CollectionCreateForm
          wrappedComponentRef={this.formRef}
          visible={visible}
          onCancel={this.handleCancel}
          onCreate={this.handleCreate}
        />
      </div>
    );
  }
}
