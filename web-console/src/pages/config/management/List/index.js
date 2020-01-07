import React, { Component } from 'react';
import { inject, observer } from 'mobx-react';
import { Link } from 'react-router-dom';
import { Button, Divider, Popconfirm } from 'antd';
import qs from 'query-string';
import PropTypes from 'prop-types';
import { upStorage } from 'up-utils';
import SearchTable from '../../../../components/SearchTable';
import NamespaceChoose from '../../../../components/NamespaceChoose';
import { CONFIG_MANAGEMENT_CREATE, CONFIG_MANAGEMENT_DETAIL } from '../../../../router/constants';

const fields = [
  {
    type: 'input',
    key: 'dataId',
    label: 'Data Id',
  },
  {
    type: 'input',
    key: 'groupName',
    label: 'Group',
  },
];

@inject(({ store: { configStore } }) => ({ configStore }))
@observer
export default class List extends Component {
  searchTableRef = React.createRef();

  static propTypes = {
    configStore: PropTypes.object.isRequired,
  };

  componentDidMount() {}

  componentWillUnmount() {
    this.props.configStore.setConfigPage();
  }

  handleDeleteConfig = dataId => () =>
    this.props.configStore.deleteConfig({ dataId }).then(this.searchTableRef.current.fetchData);

  fetchData = namespace => {
    this.props.configStore.setConfigPage();
    this.searchTableRef.current.handleReset();
    this.searchTableRef.current.fetchData({ namespace, page: 1 });
  };

  getConfigPage = params =>
    this.props.configStore.getConfigPage({
      namespace: upStorage.getSession('namespace'),
      ...params,
    });

  render() {
    const {
      configStore: {
        configPage: { page, size, totalElements, data },
      },
    } = this.props;
    const namespace = upStorage.getSession('namespace');

    const expandChildren = (
      <Link to={`${CONFIG_MANAGEMENT_CREATE}?${qs.stringify({ namespace })}`}>
        <Button type="primary">新建配置</Button>
      </Link>
    );

    const columns = [
      {
        title: 'Data Id',
        dataIndex: 'dataId',
        key: 'dataId',
      },
      {
        title: 'Group',
        dataIndex: 'groupName',
        key: 'groupName',
      },
      {
        title: '操作',
        dataIndex: 'action',
        key: 'action',
        width: '10%',
        render: (text, { dataId, groupName }) => (
          <span>
            <Link
              to={`${CONFIG_MANAGEMENT_DETAIL}?${qs.stringify({ namespace, dataId, groupName })}`}
            >
              详情
            </Link>
            <Divider type="vertical" />
            <Popconfirm
              title="确定要删除该配置吗?"
              onConfirm={this.handleDeleteConfig(dataId)}
              okText="确认"
              cancelText="取消"
            >
              <a href="#">删除</a>
            </Popconfirm>
          </span>
        ),
      },
    ];

    return (
      <div>
        <NamespaceChoose fetchData={this.fetchData} />
        <SearchTable
          firstFetch={false}
          ref={this.searchTableRef}
          expandChildren={expandChildren}
          callback={this.getConfigPage}
          fields={fields}
          tableProps={{ dataSource: data, columns, rowKey: 'dataId' }}
          paginationProps={{
            page,
            size,
            totalElements,
          }}
        />
      </div>
    );
  }
}
