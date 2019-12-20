import React, { Component } from 'react';
import { inject, observer } from 'mobx-react';
import { Form, Input, Button, Divider, Radio } from 'antd';
import { Link } from 'react-router-dom';
import qs from 'query-string';
import PropTypes from 'prop-types';
import { CONFIG_MANAGEMENT_LIST } from '../../../../router/constants';
import { CONFIG_TYPE_ENUM } from '../constants';

const { create } = Form;
const { TextArea } = Input;

const formItemLayout = {
  labelCol: {
    xs: { span: 24 },
    sm: { span: 4 },
  },
  wrapperCol: {
    xs: { span: 24 },
    sm: { span: 20 },
  },
};
const tailFormItemLayout = {
  wrapperCol: {
    xs: {
      span: 24,
      offset: 0,
    },
    sm: {
      span: 16,
      offset: 8,
    },
  },
};

@create()
@inject(({ store: { configStore } }) => ({ configStore }))
@observer
export default class Create extends Component {
  static propTypes = {
    configStore: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
    form: PropTypes.object.isRequired,
  };

  state = { loading: false };

  namespace = qs.parse(window.location.search).namespace;

  componentDidMount() {}

  componentWillUnmount() {}

  handleSubmit = e => {
    e.preventDefault();
    this.props.form.validateFieldsAndScroll((err, values) => {
      if (!err) {
        console.log('Received values of form: ', values);
        this.setState({ loading: true });
        this.props.configStore
          .createConfig({ namespace: this.namespace, ...values })
          .then(this.props.history.goBack)
          .finally(() => this.setState({ loading: false }));
      }
    });
  };

  render() {
    const { getFieldDecorator } = this.props.form;
    const { loading } = this.state;

    return (
      <Form style={{ width: 640 }} {...formItemLayout} onSubmit={this.handleSubmit}>
        <Form.Item label="Data ID">
          {getFieldDecorator('dataId', {
            rules: [
              {
                required: true,
                message: '请输入 Data ID !',
              },
            ],
          })(<Input />)}
        </Form.Item>
        <Form.Item label="Group">
          {getFieldDecorator('groupName', {
            rules: [
              {
                required: true,
                message: '请输入 Group !',
              },
            ],
          })(<Input />)}
        </Form.Item>
        <Form.Item label="配置格式">
          {getFieldDecorator('type', {
            initialValue: CONFIG_TYPE_ENUM.TEXT,
          })(
            <Radio.Group>
              {Object.values(CONFIG_TYPE_ENUM.properties).map(item => (
                <Radio key={item.value} value={item.value}>
                  {item.label}
                </Radio>
              ))}
            </Radio.Group>,
          )}
        </Form.Item>
        <Form.Item label="配置内容">
          {getFieldDecorator('content', {
            rules: [
              {
                required: true,
                message: '请输入 配置内容 !',
              },
            ],
          })(<TextArea rows={15} />)}
        </Form.Item>
        <Form.Item {...tailFormItemLayout}>
          <Button type="primary" htmlType="submit" loading={loading}>
            提交
          </Button>
          <Divider type="vertical" />
          <Link to={CONFIG_MANAGEMENT_LIST}>
            <Button>返回</Button>
          </Link>
        </Form.Item>
      </Form>
    );
  }
}
