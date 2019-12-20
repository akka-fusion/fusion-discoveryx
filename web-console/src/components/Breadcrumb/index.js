/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       面包屑导航,根据route.js生成
 * */

import React from 'react';
import { Link, withRouter } from 'react-router-dom';
import { Breadcrumb as AntdBreadcrumb, PageHeader } from 'antd';
import { breadcrumbNameMap, routes } from '../../router';
import './index.less';

const Breadcrumb = props => {
  const {
    location: { pathname },
  } = props.history;

  const pathSnippets = pathname.split('/').filter(i => i);
  const extraBreadcrumbItems = pathSnippets.map((_, index) => {
    const url = `/${pathSnippets.slice(0, index + 1).join('/')}`;

    if (!routes.find(({ path }) => path === url) || index + 1 === pathSnippets.length) {
      return <AntdBreadcrumb.Item key={url}>{breadcrumbNameMap[url]}</AntdBreadcrumb.Item>;
    }
    return (
      <AntdBreadcrumb.Item key={url}>
        <Link to={url}>{breadcrumbNameMap[url]}</Link>
      </AntdBreadcrumb.Item>
    );
  });

  const breadcrumb = <AntdBreadcrumb>{extraBreadcrumbItems}</AntdBreadcrumb>;

  return <PageHeader className="fd-breadcrumb" title={breadcrumb} onBack={props.history.goBack} />;
};

export default withRouter(props => <Breadcrumb {...props} />);
