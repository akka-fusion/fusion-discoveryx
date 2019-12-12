/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       3D旋转词云
 * */

import React, { Component } from 'react';
import { observer } from 'mobx-react';
import lodashMaxBy from 'lodash/maxBy';

const radius = 150;
const dtr = Math.PI / 180;
const d = 200;

let mcList = [];
let active = false;
let lasta = 1;
let lastb = 1;
const distr = true;
const tspeed = 5;
const size = 250;

let mouseX = 0;
let mouseY = 0;

const howElliptical = 1;

let aA = null;
let oDiv = null;
let sa;
let ca;
let sb;
let sc;
let cc;
let cb;
let per;

function depthSort() {
  const aTmp = [];

  for (let i = 0; i < aA.length; i += 1) {
    aTmp.push(aA[i]);
  }

  aTmp.sort((vItem1, vItem2) => {
    if (vItem1.cz > vItem2.cz) {
      return -1;
    } else if (vItem1.cz < vItem2.cz) {
      return 1;
    }
    return 0;
  });

  for (let i = 0; i < aTmp.length; i += 1) {
    aTmp[i].style.zIndex = i;
  }
}

function positionAll() {
  let phi = 0;
  let theta = 0;
  const max = mcList.length;

  const aTmp = [];
  const oFragment = document.createDocumentFragment();

  // //随机排序
  // for (i = 0; i < aA.length; i++) {
  //     aTmp.push(aA[i]);
  // }

  aTmp.sort(() => (Math.random() < 0.5 ? 1 : -1));

  for (let i = 0; i < aTmp.length; i += 1) {
    oFragment.appendChild(aTmp[i]);
  }

  oDiv.appendChild(oFragment);

  for (let i = 1; i < max + 1; i += 1) {
    if (distr) {
      phi = Math.acos(-1 + (2 * i - 1) / max);
      theta = Math.sqrt(max * Math.PI) * phi;
    } else {
      phi = Math.random() * Math.PI;
      theta = Math.random() * (2 * Math.PI);
    }
    // 坐标变换
    mcList[i - 1].cx = radius * Math.cos(theta) * Math.sin(phi) * 4;
    mcList[i - 1].cy = radius * Math.sin(theta) * Math.sin(phi);
    mcList[i - 1].cz = radius * Math.cos(phi) * 1.5;

    aA[i - 1].style.left = `${mcList[i - 1].cx +
      oDiv.offsetWidth / 2 -
      mcList[i - 1].offsetWidth / 2}px`;
    aA[i - 1].style.top = `${mcList[i - 1].cy +
      oDiv.offsetHeight / 2 -
      mcList[i - 1].offsetHeight / 2}px`;
  }
}

function doPosition() {
  const l = oDiv.offsetWidth / 2;
  const t = oDiv.offsetHeight / 2;
  const max = lodashMaxBy(mcList, 'size').size;
  for (let i = 0; i < mcList.length; i += 1) {
    aA[i].style.left = `${mcList[i].cx + l - mcList[i].offsetWidth / 2}px`;
    aA[i].style.top = `${mcList[i].cy + t - mcList[i].offsetHeight / 2}px`;

    // aA[i].style.fontSize=Math.ceil(12*mcList[i].scale/2)+8+'px';
    aA[i].style.fontSize = `${mcList[i].size}px`;
    aA[i].style.color = `rgb(${[
      Math.round((mcList[i].size / max) * 255),
      Math.round((mcList[i].size / max) * 166),
      Math.round((mcList[i].size / max) * 166),
    ].join(',')})`;

    aA[i].style.filter = `alpha(opacity=${100 * mcList[i].alpha})`;
    aA[i].style.opacity = mcList[i].alpha;
  }
}

function sineCosine(a, b, c) {
  sa = Math.sin(a * dtr);
  ca = Math.cos(a * dtr);
  sb = Math.sin(b * dtr);
  cb = Math.cos(b * dtr);
  sc = Math.sin(c * dtr);
  cc = Math.cos(c * dtr);
}

function update() {
  let a;
  let b;

  if (active) {
    a = (-Math.min(Math.max(-mouseY, -size), size) / radius) * tspeed;
    b = (Math.min(Math.max(-mouseX, -size), size) / radius) * tspeed;
  } else {
    a = lasta * 0.98;
    b = lastb * 0.98;
  }

  lasta = a;
  lastb = b;

  if (Math.abs(a) <= 0.01 && Math.abs(b) <= 0.01) {
    return;
  }

  const c = 0;
  // 控制旋转
  sineCosine(a, 0, c);
  for (let j = 0; j < mcList.length; j += 1) {
    const rx1 = mcList[j].cx;
    const ry1 = mcList[j].cy * ca + mcList[j].cz * -sa;
    const rz1 = mcList[j].cy * sa + mcList[j].cz * ca;

    const rx2 = rx1 * cb + rz1 * sb;
    const ry2 = ry1;
    const rz2 = rx1 * -sb + rz1 * cb;

    const rx3 = rx2 * cc + ry2 * -sc;
    const ry3 = rx2 * sc + ry2 * cc;
    const rz3 = rz2;

    mcList[j].cx = rx3;
    mcList[j].cy = ry3;
    mcList[j].cz = rz3;

    per = d / (d + rz3);

    mcList[j].x = howElliptical * rx3 * per - howElliptical * 2;
    mcList[j].y = ry3 * per;
    mcList[j].scale = per;
    mcList[j].alpha = per;

    mcList[j].alpha = (mcList[j].alpha - 0.6) * (10 / 6);
  }

  doPosition();
  depthSort();
}

function WordCloudInit(id = 'wordcloud') {
  let oTag = null;

  oDiv = document.getElementById(id);

  aA = oDiv.getElementsByTagName('a');

  for (let i = 0; i < aA.length; i += 1) {
    oTag = {};

    oTag.offsetWidth = aA[i].offsetWidth;
    oTag.offsetHeight = aA[i].offsetHeight;
    oTag.size = aA[i].id;

    mcList.push(oTag);
  }

  sineCosine(0, 0, 0);

  positionAll();

  oDiv.onmouseover = () => {
    active = true;
  };

  oDiv.onmouseout = () => {
    active = false;
  };

  oDiv.onmousemove = ev => {
    const oEvent = window.event || ev;

    mouseX = oEvent.clientX - (oDiv.offsetLeft + oDiv.offsetWidth / 2);
    mouseY = oEvent.clientY - (oDiv.offsetTop + oDiv.offsetHeight / 2);

    mouseX /= 5;
    mouseY /= 5;
  };

  setInterval(update, 30);
}

const list = [
  { label: '美女', value: '30' },
  { label: '写真', value: '11' },
  { label: '贴图', value: '12' },
  { label: '灌水', value: '13' },
  { label: '小说', value: '10' },
  { label: '大片', value: '10' },
  { label: '搞笑', value: '10' },
  { label: '壁纸', value: '14' },
  { label: '浪漫', value: '10' },
  { label: '爆笑', value: '10' },
  { label: '武侠', value: '10' },
  { label: '魔幻', value: '15' },
  { label: '教案', value: '10' },
  { label: '论文', value: '10' },
  { label: '科幻', value: '10' },
  { label: '恐怖', value: '16' },
  { label: '游戏', value: '10' },
  { label: '音乐', value: '10' },
  { label: '幽默', value: '10' },
  { label: '名校', value: '16' },
  { label: '赚钱', value: '10' },
  { label: '云计算机', value: '11' },
  { label: '美女1', value: '30' },
  { label: '写真1', value: '11' },
  { label: '贴图1', value: '12' },
  { label: '灌水1', value: '13' },
  { label: '小说1', value: '10' },
  { label: '大片1', value: '10' },
  { label: '搞笑1', value: '10' },
  { label: '壁纸1', value: '14' },
  { label: '浪漫1', value: '10' },
  { label: '爆笑1', value: '10' },
  { label: '武侠1', value: '10' },
  { label: '魔幻1', value: '15' },
  { label: '教案1', value: '10' },
  { label: '论文1', value: '10' },
  { label: '科幻1', value: '10' },
  { label: '恐怖1', value: '16' },
  { label: '游戏1', value: '10' },
  { label: '音乐1', value: '10' },
  { label: '幽默1', value: '10' },
  { label: '名校1', value: '16' },
  { label: '赚钱1', value: '10' },
  { label: '云计1算机', value: '11' },
  { label: '美女2', value: '30' },
  { label: '写真2', value: '11' },
  { label: '贴图2', value: '12' },
  { label: '灌水2', value: '13' },
  { label: '小说2', value: '10' },
  { label: '大片2', value: '10' },
  { label: '搞笑2', value: '10' },
  { label: '壁纸2', value: '14' },
  { label: '浪漫2', value: '10' },
  { label: '爆笑2', value: '10' },
  { label: '武侠2', value: '10' },
  { label: '魔幻2', value: '15' },
  { label: '教案2', value: '10' },
  { label: '论文2', value: '10' },
  { label: '科幻2', value: '10' },
  { label: '恐怖2', value: '16' },
  { label: '游戏2', value: '10' },
  { label: '音乐2', value: '10' },
  { label: '幽默2', value: '10' },
  { label: '名校2', value: '16' },
  { label: '赚钱2', value: '10' },
  { label: '云计2算机', value: '11' },
  { label: '美女3', value: '30' },
  { label: '写真3', value: '11' },
  { label: '贴图3', value: '12' },
  { label: '灌水3', value: '13' },
  { label: '小说3', value: '10' },
  { label: '大片3', value: '10' },
  { label: '搞笑3', value: '10' },
  { label: '壁纸3', value: '14' },
  { label: '浪漫3', value: '10' },
  { label: '爆笑3', value: '10' },
  { label: '武侠3', value: '10' },
  { label: '魔幻3', value: '15' },
  { label: '教案3', value: '10' },
  { label: '论文3', value: '10' },
  { label: '科幻3', value: '10' },
  { label: '恐怖3', value: '16' },
  { label: '游戏3', value: '10' },
  { label: '音乐3', value: '10' },
  { label: '幽默3', value: '10' },
  { label: '名校3', value: '16' },
  { label: '赚钱3', value: '10' },
  { label: '云计3算机', value: '11' },
];

@observer
export default class Wordcloud extends Component {
  componentDidMount() {
    WordCloudInit();
  }

  componentWillUnmount() {
    mcList = [];
  }

  renderTagbox = ({ value, label }) => (
    <a href="#" id={value} key={label}>
      {label}
    </a>
  );

  render() {
    return <div id="wordcloud">{list.map(this.renderTagbox)}</div>;
  }
}
