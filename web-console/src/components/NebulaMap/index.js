/**
 * Date              Author           Des
 *----------------------------------------------
 * 18-3-22           gongtiexin       星云图
 * */

import React, { Component } from 'react';
import { observer } from 'mobx-react';

// 圆球的构造函数
class Ball {
  constructor(radius = 40, color = '#ff0000') {
    this.radius = radius;
    this.color = color;
  }

  draw = (context, idx, p) => {
    context.save();
    context.translate(this.x, this.y);
    context.rotate(this.rotation);
    context.scale(this.scaleX, this.scaleY);
    context.lineWidth = this.lineWidth;
    context.fillStyle = this.color;
    context.beginPath();
    context.arc(0, 0, this.radius, 0, Math.PI * 2, true);
    context.closePath();
    context.fill();
    if (this.lineWidth > 0) {
      context.stroke();
    }
    context.restore();

    if (p && context.isPointInPath(p.x, p.y)) {
      console.log(idx);
    }
  };

  x = 0;

  y = 0;

  rotation = 0;

  scaleX = 1;

  scaleY = 1;

  lineWidth = 1;
}

// cancelRequestAnimFrame的兼容函数
window.cancelRequestAnimFrame = (function() {
  return (
    window.cancelAnimationFrame ||
    window.webkitCancelRequestAnimationFrame ||
    window.mozCancelRequestAnimationFrame ||
    window.oCancelRequestAnimationFrame ||
    window.msCancelRequestAnimationFrame ||
    clearTimeout
  );
})();

const data = [];
for (let i = 0; i <= 10; i += 1) {
  data.push({
    radiusX: i * 40,
    radiusY: i * 16,
  });
}

@observer
export default class NebulaMap extends Component {
  componentDidMount() {
    // 椭圆
    CanvasRenderingContext2D.prototype.oval = function(x, y, width, height) {
      const k = 0.5522848;
      const ox = width * k; // 水平控制点偏移量
      const oy = height * k; // 垂直控制点偏移量

      this.beginPath();
      // 从椭圆的左端点开始顺时针绘制四条三次贝塞尔曲线
      this.moveTo(x - width, y);
      this.bezierCurveTo(x - width, y - oy, x - ox, y - height, x, y - height);
      this.bezierCurveTo(x + ox, y - height, x + width, y - oy, x + width, y);
      this.bezierCurveTo(x + width, y + oy, x + ox, y + height, x, y + height);
      this.bezierCurveTo(x - ox, y + height, x - width, y + oy, x - width, y);
      this.closePath();
      this.stroke();
      return this;
    };

    const canvas = document.getElementById('canvas');
    const context = canvas.getContext('2d');
    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;
    const speed = 0.001;
    let angle = 0;

    function draw(p) {
      context.clearRect(0, 0, canvas.width, canvas.height);
      data.forEach(({ radiusX, radiusY }, idx) => {
        const ball = new Ball(5);
        const ball1 = new Ball(10);
        ball.x = centerX;
        ball.y = centerY;
        ball1.x = centerX;
        ball1.y = centerY;

        // 当radius的值相等时为圆周运动
        // 当radius的值不想等是为椭圆运动
        ball.x = centerX + Math.sin(angle + idx) * radiusX; // radiusX = 50
        ball.y = centerY + Math.cos(angle + idx) * radiusY; // radiusY = 100

        ball1.x = centerX + Math.sin(angle + idx + 1) * radiusX - 20; // radiusX = 50
        ball1.y = centerY + Math.cos(angle + idx + 1) * radiusY - 8; // radiusY = 100

        context.oval(centerX, centerY, radiusX, radiusY);
        ball.draw(context, idx, p);
        ball1.draw(context, `${idx}other`, p);
      });
    }

    canvas.addEventListener('click', e => draw(this.windowToCanvas(e)));

    (function drawFrame() {
      window.requestAnimationFrame(drawFrame, canvas);
      angle += speed;
      draw();
    })();
  }

  componentWillUnmount() {}

  windowToCanvas = (canvas, x, y) => {
    const bbox = canvas.getBoundingClientRect();
    return {
      x: x - bbox.left * (canvas.width / bbox.width),
      y: y - bbox.top * (canvas.height / bbox.height),
    };
  };

  render() {
    return (
      <div id="solar-system">
        <canvas
          style={{ display: 'block', margin: '0 auto' }}
          id="canvas"
          width="1200"
          height="400"
        />
      </div>
    );
  }
}
