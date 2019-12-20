/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       mobx统一注册store
 * */
import { store, hotRehydrate, rehydrate } from 'rfx-core';
import GlobalStore from './GlobalStore';
import ConfigStore from './ConfigStore';
import NamespaceStore from './NamespaceStore';
import ServiceStore from './ServiceStore';
import { isProduction } from '../utils/constants';

store.setup({
  globalStore: GlobalStore,
  configStore: ConfigStore,
  namespaceStore: NamespaceStore,
  serviceStore: ServiceStore,
});

// mobx hmr
const stores = rehydrate();
const hmrStores = isProduction ? stores : hotRehydrate();

export default hmrStores;
