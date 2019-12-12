/**
 * Date              Author           Des
 *----------------------------------------------
 * 2018/8/10           gongtiexin       通用搜索表格组件
 * */

import React, { Component } from 'react';
import { observer } from 'mobx-react';
import { withRouter } from 'react-router-dom';
import { upObject } from 'up-utils';
import PropTypes from 'prop-types';
import { Button, Col, Form, Input, Row, Table, Select, Cascader, DatePicker, Icon } from 'antd';
import Pagination from '../Pagination';
import './index.less';

const { Item: FormItem, create } = Form;
const { Option: SelectOption } = Select;
const { RangePicker } = DatePicker;

const formItemLatest = 3;

@withRouter
@create()
@observer
export default class SearchTable extends Component {
  static propTypes = {
    fields: PropTypes.array,
    tableProps: PropTypes.object.isRequired,
    paginationProps: PropTypes.object.isRequired,
    callback: PropTypes.func,
    expandChildren: PropTypes.element,
    form: PropTypes.object,
    history: PropTypes.object,
  };

  static defaultProps = {
    fields: [],
    callback: () => new Promise(resolve => setTimeout(resolve, 1000)),
    expandChildren: null,
    form: {},
    history: {},
  };

  state = {
    loading: false,
  };

  componentDidMount() {
    this.fetchData();
  }

  componentWillUnmount() {}

  handleSearch = e => {
    e.preventDefault();
    const {
      paginationProps: { pageSize },
    } = this.props;
    this.props.form.validateFields(() => {
      this.fetchData({ sliceParams: { pageSize, pageNum: 1 } });
    });
  };

  handleReset = () => this.props.form.resetFields();

  fetchData = (params = {}) => {
    this.setState({ loading: true });

    const {
      paginationProps: { pageSize, currentPage },
    } = this.props;
    const formValues = this.props.form.getFieldsValue();
    const newParams = upObject.filterNull({
      sliceParams: { pageSize, currentPage }, // 分页参数
      ...formValues, // 搜索条件
      ...params, // 其它参数
    });

    Promise.resolve(this.props.callback(newParams))
      .then(() => {
        const {
          tableProps: { dataSource: nextDataSource },
          paginationProps: { currentPage: nextCurrentPage },
        } = this.props;
        // 当前页没有数据又有上一页，自动翻到上一页
        if (nextDataSource.length === 0 && nextCurrentPage > 1) {
          newParams.currentPage = nextCurrentPage - 1;
          this.fetchData(newParams);
          return;
        }
        this.props.history.replace({ ...this.props.history.location, state: newParams });
      })
      .finally(() => this.setState({ loading: false }));
  };

  getFormItemByType = ({ type, key, label, items = [], options, props }) => {
    const {
      form: { getFieldDecorator },
    } = this.props;
    switch (type) {
      case 'input': {
        return (
          <FormItem label={label}>{getFieldDecorator(key, options)(<Input {...props} />)}</FormItem>
        );
      }
      case 'select': {
        return (
          <FormItem label={label}>
            {getFieldDecorator(
              key,
              options,
            )(
              <Select {...props}>
                {items.map(item => (
                  <SelectOption key={item.key || item.value} value={item.value}>
                    {item.label}
                  </SelectOption>
                ))}
              </Select>,
            )}
          </FormItem>
        );
      }
      case 'datePicker': {
        return (
          <FormItem>
            {getFieldDecorator(
              key,
              options,
            )(<DatePicker style={{ width: '100%' }} allowClear placeholder={label} {...props} />)}
          </FormItem>
        );
      }
      case 'rangePicker': {
        return <FormItem>{getFieldDecorator(key, options)(<RangePicker />)}</FormItem>;
      }
      case 'cascader': {
        return (
          <FormItem>
            {getFieldDecorator(key, options)(<Cascader placeholder={label} {...props} />)}
          </FormItem>
        );
      }
      default:
        return null;
    }
  };

  renderFormItems = () => {
    const { fields } = this.props;
    const count = this.state.expand ? fields.length : formItemLatest;
    return fields.map((field, i) => (
      <Col span={8} key={field.key} style={{ display: i < count ? 'block' : 'none' }}>
        {this.getFormItemByType(field)}
      </Col>
    ));
  };

  toggle = () => {
    const { expand } = this.state;
    this.setState({ expand: !expand });
  };

  render() {
    const { fields = [], tableProps, paginationProps, expandChildren } = this.props;
    const { loading } = this.state;

    return (
      <div className="search-table">
        {fields.length > 0 && (
          <Form className="ant-advanced-search-form" onSubmit={this.handleSearch}>
            <Row gutter={24}>{this.renderFormItems()}</Row>
            <Row>
              <Col span={24} style={{ textAlign: 'right' }}>
                <Button type="primary" htmlType="submit">
                  Search
                </Button>
                <Button style={{ marginLeft: 8 }} onClick={this.handleReset}>
                  Clear
                </Button>
                <a style={{ marginLeft: 8, fontSize: 12 }} onClick={this.toggle}>
                  展开 <Icon type={this.state.expand ? 'up' : 'down'} />
                </a>
              </Col>
            </Row>
          </Form>
        )}
        {expandChildren && <div className="search-expand">{expandChildren}</div>}
        <Table size="middle" bordered loading={loading} pagination={false} {...tableProps} />
        <Pagination handleChange={this.fetchData} {...paginationProps} />
      </div>
    );
  }
}
