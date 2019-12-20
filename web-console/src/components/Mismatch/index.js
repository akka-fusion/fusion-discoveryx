/**
 * Date              Author           Des
 *----------------------------------------------
 * 2018/6/20           gongtiexin       404
 * */

import React from 'react';
import './index.less';

const Mismatch = () => (
  <div id="noMatch">
    <span>404</span>
    <p>抱歉，你访问的页面不存在</p>
  </div>
);

export default React.memo(Mismatch);
