/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       常量
 * */
const browserRedirect = () => {
  const sUserAgent = navigator.userAgent.toLowerCase();
  const bIsIpad = sUserAgent.includes('ipad');
  const bIsIphoneOs = sUserAgent.includes('iphone os');
  const bIsMidp = sUserAgent.includes('midp');
  const bIsUc7 = sUserAgent.includes('rv:1.2.3.4');
  const bIsUc = sUserAgent.includes('ucweb');
  const bIsAndroid = sUserAgent.includes('android');
  const bIsCE = sUserAgent.includes('windows ce');
  const bIsWM = sUserAgent.includes('windows mobile');
  if (bIsIpad || bIsIphoneOs || bIsMidp || bIsUc7 || bIsUc || bIsAndroid || bIsCE || bIsWM) {
    // phone
    return 'phone';
  }
  // pc
  return 'pc';
};

const isProduction = process.env.NODE_ENV === 'production';

const ECHARTS_DEFULT_OPTION = {
  tooltip: {
    trigger: 'axis',
  },
  legend: {},
  xAxis: {
    type: 'category',
  },
  yAxis: [
    {
      type: 'value',
      name: '数量',
      nameTextStyle: {
        color: '#000000a6',
      },
      splitLine: { show: false },
    },
    {
      type: 'value',
      splitLine: { show: false },
      name: '百分比',
      min: 0,
      max: 100,
      interval: 20,
      axisLabel: {
        formatter: '{value} %',
      },
      nameTextStyle: {
        color: '#000000a6',
      },
    },
  ],
  series: [],
};

export { isProduction, ECHARTS_DEFULT_OPTION, browserRedirect };
