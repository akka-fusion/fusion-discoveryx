import React, { Component } from 'react';
import { inject, observer } from 'mobx-react';
import { Link } from 'react-router-dom';
import { Button, Divider, Popconfirm, Tabs } from 'antd';
import qs from 'query-string';
import PropTypes from 'prop-types';
import SearchTable from '../../../../components/SearchTable';
import { CONFIG_MANAGEMENT_CREATE } from '../../../../router/constants';

const { TabPane } = Tabs;

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

@inject(({ store: { configStore, namespaceStore } }) => ({ configStore, namespaceStore }))
@observer
export default class List extends Component {
  static propTypes = {
    configStore: PropTypes.object.isRequired,
    namespaceStore: PropTypes.object.isRequired,
  };

  state = { namespace: null };

  searchTableRef = React.createRef();

  componentDidMount() {}

  componentWillUnmount() {
    this.props.configStore.setConfigPage();
    this.props.namespaceStore.setNamespaceList();
  }

  handleDeleteConfig = dataId => () =>
    this.props.configStore.deleteConfig({ dataId }).then(this.searchTableRef.current.fetchData);

  handleTabsChange = namespace => {
    this.props.configStore.setConfigPage();
    this.searchTableRef.current.handleReset();
    this.searchTableRef.current.fetchData({ namespace, page: 1 });
    this.setState({ namespace });
  };

  getConfigPage = async params => {
    const namespace =
      this.state.namespace ||
      (await this.props.namespaceStore.getNamespaceList().then(namespaceList => {
        const $namespace = namespaceList[0]?.namespace;
        this.setState({ namespace: $namespace });
        return $namespace;
      }));
    return this.props.configStore.getConfigPage({ namespace, ...params });
  };

  render() {
    const {
      configStore: {
        configPage: { page, size, totalElements, data },
      },
      namespaceStore: { namespaceList },
    } = this.props;
    const { namespace } = this.state;

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
        render: (text, record) => (
          <span>
            <a>详情</a>
            <Divider type="vertical" />
            <Popconfirm
              title="确定要删除该配置吗?"
              onConfirm={this.handleDeleteConfig(record.dataId)}
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
      <div id="config">
        <Tabs onChange={this.handleTabsChange}>
          {namespaceList.map(item => (
            <TabPane tab={item.name} key={item.namespace} />
          ))}
        </Tabs>
        <SearchTable
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
