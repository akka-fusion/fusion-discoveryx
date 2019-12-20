/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       分页组件
 * */

import React, { Component } from 'react';
import { observer } from 'mobx-react';
import { Pagination as AntdPagination } from 'antd';
import PropTypes from 'prop-types';
import './index.less';

@observer
export default class Pagination extends Component {
  static propTypes = {
    page: PropTypes.number.isRequired,
    size: PropTypes.number,
    totalElements: PropTypes.number.isRequired,
    handleChange: PropTypes.func.isRequired,
    showSizeChanger: PropTypes.bool,
  };

  static defaultProps = {
    size: 10,
    showSizeChanger: true,
  };

  onShowSizeChange = (current, pageSize) => {
    const { handleChange } = this.props;
    handleChange({ page: current, size: pageSize });
  };

  onChange = (pageNumber, pageSize) => {
    const { handleChange } = this.props;
    handleChange({ page: pageNumber, size: pageSize });
  };

  showTotal = total => `共 ${total || 0} 条`;

  render() {
    const { page, size, totalElements, showSizeChanger } = this.props;

    return (
      <AntdPagination
        current={page}
        pageSize={size}
        className="rs-pagination"
        showSizeChanger={showSizeChanger || false}
        onShowSizeChange={this.onShowSizeChange}
        showQuickJumper
        onChange={this.onChange}
        showTotal={this.showTotal}
        total={totalElements}
      />
    );
  }
}
