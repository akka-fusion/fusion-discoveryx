import React, { Component } from 'react';
import { Link, Route, Switch } from 'react-router-dom';
import { observer } from 'mobx-react';
import { Layout, Menu } from 'antd';
import './index.less';
import { routes } from '../../router';
import { PrivateRoute } from '../../router/feature';
import Loadable from '../../components/Loadable';
import Breadcrumb from '../../components/Breadcrumb';
import {
  CONFIG_MANAGEMENT_LIST,
  NAMESPACE_MANAGEMENT_LIST,
  SERVICE_MANAGEMENT_LIST,
} from '../../router/constants';

const { Header, Content, Sider, Footer } = Layout;
const { SubMenu } = Menu;

const LoadableMismatch = Loadable({
  loader: () => import(/* webpackChunkName: "route-mismatch" */ '../../components/Mismatch'),
});

@observer
export default class App extends Component {
  componentDidMount() {}

  renderRoute = ({ path, component }) => (
    <PrivateRoute key={path} path={path} component={component} exact />
  );

  render() {
    return (
      <Layout id="app" style={{ minHeight: '100vh' }}>
        <Sider>
          <div className="logo">Fusion DiscoveryX</div>
          <Menu theme="dark" mode="inline">
            <SubMenu key="config" title="配置管理">
              <Menu.Item key={CONFIG_MANAGEMENT_LIST}>
                <Link to={CONFIG_MANAGEMENT_LIST}>配置列表</Link>
              </Menu.Item>
            </SubMenu>
            <SubMenu key="service" title="服务管理">
              <Menu.Item key={SERVICE_MANAGEMENT_LIST}>
                <Link to={SERVICE_MANAGEMENT_LIST}>服务列表</Link>
              </Menu.Item>
              {/* <Menu.Item key="/subscriberList"> */}
              {/*  <Link to="/subscriberList">订阅者列表</Link> */}
              {/* </Menu.Item> */}
            </SubMenu>
            <Menu.Item key={NAMESPACE_MANAGEMENT_LIST}>
              <Link to={NAMESPACE_MANAGEMENT_LIST}>命名空间</Link>
            </Menu.Item>
            {/* <SubMenu key="cluster" title="集群管理"> */}
            {/*  <Menu.Item key="/clusterManagement"> */}
            {/*    <Link to="/clusterManagement">节点列表</Link> */}
            {/*  </Menu.Item> */}
            {/* </SubMenu> */}
          </Menu>
        </Sider>
        <Layout>
          <Header
            style={{
              background: '#fff',
              padding: 0,
              boxShadow: '0 1px 4px rgba(0,21,41,.08)',
              zIndex: '2',
            }}
          >
            <span>首页</span>
            <span>文档</span>
            <span>博客</span>
            <span>社区</span>
          </Header>
          <Breadcrumb />
          <Content style={{ margin: '0 16px', display: 'flex', flexDirection: 'column' }}>
            <div
              style={{
                padding: 24,
                position: 'relative',
                flex: '1 1 auto',
                backgroundColor: '#fff',
              }}
            >
              <Switch>
                {routes.map(this.renderRoute)}
                <Route component={LoadableMismatch} />
              </Switch>
            </div>
          </Content>
          <Footer style={{ textAlign: 'center' }}>Ant Design ©2018 Created by Ant UED</Footer>
        </Layout>
      </Layout>
    );
  }
}
