import React, { Component } from 'react';
import { inject, observer } from 'mobx-react';
import { Button, Modal, Form, Input, Divider, Popconfirm } from 'antd';
import PropTypes from 'prop-types';
import SearchTable from '../../../../components/SearchTable';

const fields = [
  {
    type: 'input',
    key: 'account',
    label: 'account',
  },
  {
    type: 'input',
    key: 'name',
    label: 'name',
  },
];

const CollectionCreateForm = Form.create({ name: 'form_in_modal' })(
  class extends React.Component {
    render() {
      const { visible, onCancel, onCreate, form, user } = this.props;
      const { getFieldDecorator } = form;
      return (
        <Modal
          visible={visible}
          title={`${user ? '编辑' : '新建'}用户`}
          onCancel={onCancel}
          onOk={onCreate}
        >
          <Form layout="vertical">
            <Form.Item label="account">
              {getFieldDecorator('account', {
                rules: [{ required: true, message: '请输入 account !' }],
                initialValue: user?.account,
              })(<Input disabled={!!user} />)}
            </Form.Item>
            <Form.Item label="name">
              {getFieldDecorator('name', {
                rules: [{ required: true, message: '请输入 name !' }],
                initialValue: user?.name,
              })(<Input />)}
            </Form.Item>
            <Form.Item label="password">
              {getFieldDecorator('password', {
                rules: [{ required: !user, message: '请输入 password !' }],
                initialValue: user?.password,
              })(<Input />)}
            </Form.Item>
          </Form>
        </Modal>
      );
    }
  },
);

@inject(({ store: { userStore } }) => ({ userStore }))
@observer
export default class List extends Component {
  static propTypes = {
    userStore: PropTypes.object.isRequired,
  };

  state = {
    visible: false,
    user: null,
  };

  searchTableRef = React.createRef();

  formRef = React.createRef();

  componentDidMount() {}

  componentWillUnmount() {
    this.props.userStore.setUserPage();
  }

  handleDeleteUser = user => () =>
    this.props.userStore.deleteUser({ user }).then(this.searchTableRef.current.fetchData);

  showModal = user => () => {
    this.setState({ visible: true, user });
  };

  handleCancel = () => {
    this.setState({ visible: false, user: null });
    this.formRef.current.props.form.resetFields();
  };

  handleCreate = () => {
    const { form } = this.formRef.current.props;
    const { user } = this.state;

    form.validateFields((err, values) => {
      if (err) {
        return;
      }
      console.log('Received values of form: ', values);
      this.props.userStore[`${user ? 'update' : 'create'}User`]({
        ...user,
        ...values,
      }).then(() => {
        this.searchTableRef.current.fetchData();
        this.handleCancel();
      });
    });
  };

  render() {
    const {
      userStore: {
        userPage: { page, size, totalElements, data },
      },
    } = this.props;

    const { visible, user } = this.state;

    const columns = [
      {
        title: 'account',
        dataIndex: 'account',
        key: 'account',
      },
      {
        title: 'name',
        dataIndex: 'name',
        key: 'name',
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
              title="确定要删除该用户吗?"
              onConfirm={this.handleDeleteUser(record.user)}
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
        新建用户
      </Button>
    );

    return (
      <div>
        <SearchTable
          fields={fields}
          ref={this.searchTableRef}
          expandChildren={expandChildren}
          callback={this.props.userStore.getUserPage}
          tableProps={{ dataSource: data, columns, rowKey: 'account' }}
          paginationProps={{ size, page, totalElements }}
        />
        <CollectionCreateForm
          wrappedComponentRef={this.formRef}
          visible={visible}
          user={user}
          onCancel={this.handleCancel}
          onCreate={this.handleCreate}
        />
      </div>
    );
  }
}
