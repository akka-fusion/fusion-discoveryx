/**
 * Date              Author           Des
 *----------------------------------------------
 * 2018/5/18           gongtiexin       登陆组件
 * */

import React, { Component } from 'react';
import { observer } from 'mobx-react';
import { Link } from 'react-router-dom';
import PropTypes from 'prop-types';
import { Form, Icon, Input, Button, Checkbox } from 'antd';
import Particles from 'particlesjs';
import './index.less';
import { browserRedirect } from '../../utils/constants';

const { Item: FormItem, create } = Form;

@create()
@observer
export default class Login extends Component {
  static propTypes = {
    form: PropTypes.object.isRequired,
  };

  componentDidMount() {
    this.particlesInit();
  }

  particlesInit = () => {
    if (browserRedirect() === 'pc') {
      Particles.init({
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
      }
    });
  };

  render() {
    const { getFieldDecorator } = this.props.form;
    return (
      <div id="login">
        <canvas className="particles-background" />
        <div className="login-box">
          <div className="message">单点登陆</div>
          <div id="darkbannerwrap" />
          <Form onSubmit={this.handleSubmit} className="login-form">
            <FormItem>
              {getFieldDecorator('userName', {
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
              <Link className="login-form-forgot" to="/forgot">
                忘记密码
              </Link>
              <Button type="primary" htmlType="submit" className="login-form-button">
                Log in
              </Button>
              或者
              <Link to="/register">现在去注册!</Link>
            </FormItem>
          </Form>
        </div>
      </div>
    );
  }
}
