/**
 * Date              Author           Des
 *----------------------------------------------
 * 2018/5/18           gongtiexin       登陆组件
 * */

import React, { Component } from 'react';
import { inject, observer } from 'mobx-react';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import { Button, Checkbox, Form, Icon, Input } from 'antd';
import Particles from 'particlesjs';
import './index.less';
import { browserRedirect } from '../../utils/constants';
import { NAMESPACE_MANAGEMENT_LIST } from '../../router/constants';

const { Item: FormItem, create } = Form;

@create()
@inject(({ store: { userStore } }) => ({ userStore }))
@observer
export default class Login extends Component {
  state = {
    loading: false,
  };

  particles = null;

  static propTypes = {
    form: PropTypes.object.isRequired,
    userStore: PropTypes.object.isRequired,
    history: PropTypes.object.isRequired,
  };

  componentDidMount() {
    this.particlesInit();
  }

  componentWillUnmount() {
    this.particles?.destroy();
  }

  particlesInit = () => {
    if (browserRedirect() === 'pc') {
      this.particles = Particles.init({
        selector: '.particles-background',
        connectParticles: true,
        color: '#999999',
        maxParticles: 150,
      });
    }
  };

  handleSubmit = e => {
    e.preventDefault();
    this.props.form.validateFields((err, values) => {
      if (!err) {
        console.log('Received values of form: ', values);
        this.setState({ loading: true });
        this.props.userStore
          .login(values)
          .then(() => this.props.history.push(NAMESPACE_MANAGEMENT_LIST))
          .finally(() => this.setState({ loading: false }));
      }
    });
  };

  render() {
    const { getFieldDecorator } = this.props.form;
    const { loading } = this.state;
    return (
      <div id="login">
        <canvas className="particles-background" />
        <div className="login-box">
          <div className="message">Fusion DiscoveryX</div>
          <div id="darkbannerwrap" />
          <Form onSubmit={this.handleSubmit} className="login-form">
            <FormItem>
              {getFieldDecorator('account', {
                rules: [{ required: true, message: '请输入您的用户名!' }],
              })(
                <Input
                  prefix={<Icon type="user" style={{ color: 'rgba(0,0,0,.25)' }} />}
                  placeholder="用户名"
                  className="login-form-input"
                />,
              )}
            </FormItem>
            <FormItem>
              {getFieldDecorator('password', {
                rules: [{ required: true, message: '请输入您的密码!' }],
              })(
                <Input
                  prefix={<Icon type="lock" style={{ color: 'rgba(0,0,0,.25)' }} />}
                  type="password"
                  placeholder="密码"
                  className="login-form-input"
                />,
              )}
            </FormItem>
            <FormItem>
              {getFieldDecorator('remember', {
                valuePropName: 'checked',
                initialValue: true,
              })(<Checkbox>记住密码</Checkbox>)}
              <Link className="login-form-forgot" to="/#">
                忘记密码
              </Link>
              <Button
                loading={loading}
                type="primary"
                htmlType="submit"
                className="login-form-button"
              >
                登录
              </Button>
              或者
              <Link to="/#">现在去注册!</Link>
            </FormItem>
          </Form>
        </div>
      </div>
    );
  }
}
