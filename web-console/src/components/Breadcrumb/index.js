/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       面包屑导航,根据route.js生成
 * */

import React, { Component } from 'react';
import { Link } from 'react-router-dom';
import { inject, observer } from 'mobx-react';
import { Breadcrumb as AntdBreadcrumb } from 'antd';
import PropTypes from 'prop-types';
import { breadcrumbNameMap, routes } from '../../router';

@inject('store')
@observer
export default class Breadcrumb extends Component {
  static propTypes = {
    location: PropTypes.object.isRequired,
  };

  render() {
    const {
      location: { pathname },
    } = this.props;
    const pathSnippets = pathname.split('/').filter(i => i);
    const extraBreadcrumbItems = pathSnippets.map((_, index) => {
      const url = `/${pathSnippets.slice(0, index + 1).join('/')}`;

      if (!routes.find(({ path }) => path === url)) {
        return <AntdBreadcrumb.Item key={url}>{breadcrumbNameMap[url]}</AntdBreadcrumb.Item>;
      }
      return (
        <AntdBreadcrumb.Item key={url}>
          <Link to={url}>{breadcrumbNameMap[url]}</Link>
        </AntdBreadcrumb.Item>
      );
    });

    const breadcrumbItems = [
      <AntdBreadcrumb.Item key="app">
        <Link to="/">首页</Link>
      </AntdBreadcrumb.Item>,
    ].concat(extraBreadcrumbItems);

    return <AntdBreadcrumb style={{ margin: '16px 0' }}>{breadcrumbItems}</AntdBreadcrumb>;
  }
}
