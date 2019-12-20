import React, { Component } from 'react';
import { inject, observer } from 'mobx-react';
import { Button, Modal, Form, Input, Divider, Popconfirm } from 'antd';
import PropTypes from 'prop-types';
import SearchTable from '../../../../components/SearchTable';

const CollectionCreateForm = Form.create({ name: 'form_in_modal' })(
  class extends React.Component {
    render() {
      const { visible, onCancel, onCreate, form, namespace } = this.props;
      const { getFieldDecorator } = form;
      return (
        <Modal
          visible={visible}
          title={`${namespace ? '编辑' : '创建'}命名空间`}
          onCancel={onCancel}
          onOk={onCreate}
        >
          <Form layout="vertical">
            <Form.Item label="命名空间名称">
              {getFieldDecorator('name', {
                rules: [{ required: true, message: '请输入命名空间名称!' }],
                initialValue: namespace?.name,
              })(<Input />)}
            </Form.Item>
          </Form>
        </Modal>
      );
    }
  },
);

@inject(({ store: { namespaceStore } }) => ({ namespaceStore }))
@observer
export default class List extends Component {
  static propTypes = {
    namespaceStore: PropTypes.object.isRequired,
  };

  state = {
    visible: false,
    namespace: null,
  };

  searchTableRef = React.createRef();

  formRef = React.createRef();

  componentDidMount() {}

  componentWillUnmount() {
    this.props.namespaceStore.setNamespacePage();
  }

  handleDeleteNamespace = namespace => () =>
    this.props.namespaceStore
      .deleteNamespace({ namespace })
      .then(this.searchTableRef.current.fetchData);

  showModal = namespace => () => {
    this.setState({ visible: true, namespace });
  };

  handleCancel = () => {
    this.setState({ visible: false, namespace: null });
    this.formRef.current.props.form.resetFields();
  };

  handleCreate = () => {
    const { form } = this.formRef.current.props;
    const { namespace } = this.state;

    form.validateFields((err, values) => {
      if (err) {
        return;
      }
      console.log('Received values of form: ', values);
      this.props.namespaceStore[`${namespace ? 'update' : 'create'}Namespace`]({
        ...namespace,
        ...values,
      }).then(() => {
        this.searchTableRef.current.fetchData();
        this.handleCancel();
      });
    });
  };

  render() {
    const {
      namespaceStore: {
        namespacePage: { page, size, totalElements, data },
      },
    } = this.props;

    const { visible, namespace } = this.state;

    const columns = [
      {
        title: '命名空间名称',
        dataIndex: 'name',
        key: 'name',
      },
      {
        title: '命名空间ID',
        dataIndex: 'namespace',
        key: 'namespace',
      },
      {
        title: '操作',
        dataIndex: 'action',
        key: 'action',
        width: '10%',
        render: (text, record) => (
          <span>
            <a href="#" onClick={this.showModal(record)}>
              编辑
            </a>
            <Divider type="vertical" />
            <Popconfirm
              title="确定要删除该命名空间吗?"
              onConfirm={this.handleDeleteNamespace(record.namespace)}
              okText="确认"
              cancelText="取消"
            >
              <a href="#">删除</a>
            </Popconfirm>
          </span>
        ),
      },
    ];

    const expandChildren = (
      <Button type="primary" onClick={this.showModal()}>
        新建命名空间
      </Button>
    );

    return (
      <div id="config">
        <SearchTable
          ref={this.searchTableRef}
          expandChildren={expandChildren}
          callback={this.props.namespaceStore.getNamespacePage}
          tableProps={{ dataSource: data, columns, rowKey: 'namespace' }}
          paginationProps={{ size, page, totalElements }}
        />
        <CollectionCreateForm
          wrappedComponentRef={this.formRef}
          visible={visible}
          namespace={namespace}
          onCancel={this.handleCancel}
          onCreate={this.handleCreate}
        />
      </div>
    );
  }
}
