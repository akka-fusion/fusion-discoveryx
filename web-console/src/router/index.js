/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       配置路由
 * */

import Loadable from '../components/Loadable';
import {
  CONFIG_MANAGEMENT_CREATE,
  CONFIG_MANAGEMENT_DETAIL,
  CONFIG_MANAGEMENT_LIST,
  NAMESPACE_MANAGEMENT_LIST,
  SERVICE_MANAGEMENT_DETAIL,
  SERVICE_MANAGEMENT_LIST,
  USER_MANAGEMENT_LIST,
} from './constants';

const routes = [
  {
    path: CONFIG_MANAGEMENT_LIST,
    component: Loadable({
      loader: () =>
        import(
          /* webpackChunkName: "route-config-management-list" */ '../pages/config/management/List'
        ),
    }),
  },
  {
    path: CONFIG_MANAGEMENT_CREATE,
    component: Loadable({
      loader: () =>
        import(
          /* webpackChunkName: "route-config-management-create" */ '../pages/config/management/Create'
        ),
    }),
  },
  {
    path: CONFIG_MANAGEMENT_DETAIL,
    component: Loadable({
      loader: () =>
        import(
          /* webpackChunkName: "route-config-management-detail" */ '../pages/config/management/Detail'
        ),
    }),
  },
  {
    path: NAMESPACE_MANAGEMENT_LIST,
    component: Loadable({
      loader: () =>
        import(
          /* webpackChunkName: "route-namespace-management-list" */ '../pages/namespace/management/List'
        ),
    }),
  },
  {
    path: SERVICE_MANAGEMENT_LIST,
    component: Loadable({
      loader: () =>
        import(
          /* webpackChunkName: "route-service-management-list" */ '../pages/service/management/List'
        ),
    }),
  },
  {
    path: SERVICE_MANAGEMENT_DETAIL,
    component: Loadable({
      loader: () =>
        import(
          /* webpackChunkName: "route-service-management-detail" */ '../pages/service/management/Detail'
        ),
    }),
  },
  {
    path: USER_MANAGEMENT_LIST,
    component: Loadable({
      loader: () =>
        import(
          /* webpackChunkName: "route-user-management-list" */ '../pages/user/management/List'
        ),
    }),
  },
];

const breadcrumbNameMap = {
  [CONFIG_MANAGEMENT_LIST]: '配置列表',
  [CONFIG_MANAGEMENT_CREATE]: '新建配置',
  [CONFIG_MANAGEMENT_DETAIL]: '配置详情',
  [NAMESPACE_MANAGEMENT_LIST]: '命名空间',
  [SERVICE_MANAGEMENT_LIST]: '服务列表',
  [SERVICE_MANAGEMENT_DETAIL]: '服务详情',
  [USER_MANAGEMENT_LIST]: '用户管理',
};

export { routes, breadcrumbNameMap };
