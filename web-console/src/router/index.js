/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       配置路由
 * */

import Loadable from '../components/Loadable';

const routes = [
  // {
  //   path: '/',
  //   component: Loadable({
  //     loader: () => import(/* webpackChunkName: "route-root" */ '../pages/App'),
  //   }),
  // },
];

const breadcrumbNameMap = {
  '/route': 'label',
};

export { routes, breadcrumbNameMap };
