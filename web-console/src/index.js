/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       单页应用的入口文件
 * */
import 'core-js/stable';
import 'regenerator-runtime/runtime';

import React from 'react';
import { render } from 'react-dom';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import { Provider } from 'mobx-react';
import { hotRehydrate, rehydrate } from 'rfx-core';
import moment from 'moment';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/lib/locale-provider/zh_CN';
import 'normalize.css';
import { isProduction } from './utils/constants';
import './stores';
import './global.less';
import Loadable from './components/Loadable';

/**
 * 代码拆分和按需加载
 */
const LoadableApp = Loadable({
  loader: () => import(/* webpackChunkName: "route-root" */ './pages/App'),
});

const LoadableLogin = Loadable({
  loader: () => import(/* webpackChunkName: "route-login" */ './components/Login'),
});

/**
 * moment时区设置为中国
 */
moment.locale('zh-cn');

const store = rehydrate();

const renderApp = () => {
  render(
    <Provider store={isProduction ? store : hotRehydrate()}>
      <ConfigProvider locale={zhCN}>
        <Router>
          <Switch>
            <Route path="/login" component={LoadableLogin} exact />
            <Route path="/" component={LoadableApp} />
          </Switch>
        </Router>
      </ConfigProvider>
    </Provider>,
    document.getElementById('root'),
  );
};

function run() {
  renderApp();
  if (module.hot) {
    module.hot.accept(renderApp);
  }
}

store.userStore.getCurrentSessionUser().then(run, run);
