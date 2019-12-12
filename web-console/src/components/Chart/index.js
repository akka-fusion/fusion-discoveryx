/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       React Echarts Component
 *
 * option: {
 *   option: 基础配置项,
 *   data: 数据源,
 *   row: 横坐标上的属性(对于data里面的key),
 *   column: 纵坐标上的属性(对于data里面的key),
 *   value: 图上的属性(对于data里面的key),
 *   seriesTemplates: 每个series的配置
 *   }
 * */

import React, { useEffect, useRef } from 'react';
import echarts from 'echarts';
import { computedEchartsOption } from 'up-utils';
import lodashIsEqual from 'lodash/isEqual';
import * as PropTypes from 'prop-types';

const EChart = props => {
  const { option, style } = props;
  const echartsRef = useRef(null);
  let echartsInstance;

  const renderChart = () => {
    if (echartsRef.current) {
      const renderedInstance = echarts.getInstanceByDom(echartsRef.current);
      if (renderedInstance) {
        echartsInstance = renderedInstance;
      } else {
        echartsInstance = echarts.init(echartsRef.current);
      }
      echartsInstance.setOption(computedEchartsOption(option));
    }
  };

  useEffect(() => {
    const handleWindowResize = () => {
      console.log('echarts resize');
      echartsInstance.resize();
    };
    window.addEventListener('resize', handleWindowResize);
    return () => {
      window.removeEventListener('resize', handleWindowResize);
      echartsInstance.dispose();
      console.log('echarts dispose');
    };
  }, []);

  useEffect(() => {
    renderChart();
    console.log('echarts render');
  });

  return <div ref={echartsRef} style={style} />;
};

const areEqual = (prevProps, nextProps) => {
  return lodashIsEqual(prevProps.option.data, nextProps.option.data);
};

EChart.propTypes = {
  option: PropTypes.object.isRequired,
  style: PropTypes.object,
};

EChart.defaultProps = {
  style: { width: '100%', height: 400 },
};

export default React.memo(EChart, areEqual);
