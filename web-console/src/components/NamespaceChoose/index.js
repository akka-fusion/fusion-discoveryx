import React, { Component } from 'react';
import { inject, observer } from 'mobx-react';
import { Tabs } from 'antd';
import PropTypes from 'prop-types';
import { upStorage } from 'up-utils';

const { TabPane } = Tabs;

@inject(({ store: { namespaceStore } }) => ({ namespaceStore }))
@observer
export default class NamespaceChoose extends Component {
  static propTypes = {
    namespaceStore: PropTypes.object.isRequired,
    fetchData: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);
    this.state = { namespace: upStorage.getSession('namespace') };
  }

  componentDidMount() {
    this.props.namespaceStore.getNamespaceList().then(namespaceList => {
      let { namespace } = this.state;
      if (!namespaceList.some(item => item.namespace === upStorage.getSession('namespace'))) {
        namespace = namespaceList[0]?.namespace;
      }
      this.handleNamespaceChange(namespace);
    });
  }

  componentWillUnmount() {
    this.props.namespaceStore.setNamespaceList();
  }

  handleNamespaceChange = namespace => {
    this.setState({ namespace });
    upStorage.setSession('namespace', namespace);
    this.props.fetchData(namespace);
  };

  render() {
    const {
      namespaceStore: { namespaceList },
    } = this.props;
    const { namespace } = this.state;

    return (
      <Tabs activeKey={namespace} onChange={this.handleNamespaceChange}>
        {namespaceList.map(item => (
          <TabPane tab={item.name} key={item.namespace} />
        ))}
      </Tabs>
    );
  }
}
